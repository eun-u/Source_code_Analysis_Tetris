package kr.ac.jbnu.se.tetris.firebase.server;

import static kr.ac.jbnu.se.tetris.firebase.server.FirestoreSchema.*;

import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.Transaction;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import kr.ac.jbnu.se.tetris.auth.AuthException;
import kr.ac.jbnu.se.tetris.auth.UsernameIdentity;

/** Binds verified Auth identities to permanent game IDs; public Auth profile edits grant no new name. */
final class FirebaseIdentityRegistry {
    private final Firestore firestore;
    FirebaseIdentityRegistry(Firestore firestore) { this.firestore = firestore; }

    String resolve(String uid, String email, boolean migrated) throws IOException, AuthException {
        final String username = UsernameIdentity.fromEmail(email);
        if (email == null || (username == null && !migrated)) throw new AuthException("GAME_ID_REQUIRED");
        String key = Base64.getUrlEncoder().withoutPadding().encodeToString(uid.getBytes(StandardCharsets.UTF_8));
        DocumentReference identity = firestore.collection(RANKED_IDENTITIES).document(key);
        DocumentReference name = username == null ? null : firestore.collection(RANKED_USERNAMES).document(username);
        ApiFuture<String> operation = firestore.runTransaction(
                tx -> bindInTransaction(tx, identity, name, uid, email, username));
        return await(operation);
    }

    /** 신원 문서와 아이디 예약 문서를 함께 확인하고, 처음 보는 계정이면 둘 다 만든다. */
    private static String bindInTransaction(Transaction tx, DocumentReference identity, DocumentReference name,
            String uid, String email, String username) throws Exception {
        DocumentSnapshot existing = tx.get(identity).get();
        DocumentSnapshot reserved = name == null ? null : tx.get(name).get();
        if (existing.exists()) {
            if (!uid.equals(existing.getString(USER_ID)) || !email.equalsIgnoreCase(existing.getString(EMAIL))) {
                throw new IdentityRejected("GAME_ID_CHANGED");
            }
        } else if (username == null) {
            // Only the migration tool may seed a real-email legacy account and its trusted name.
            throw new IdentityRejected("LEGACY_IDENTITY_REQUIRED");
        }
        if (reserved != null && reserved.exists() && !uid.equals(reserved.getString(USER_ID))) {
            throw new IdentityRejected("GAME_ID_RESERVED");
        }
        String displayName = existing.exists() ? existing.getString(DISPLAY_NAME) : username;
        if (!isValidDisplayName(displayName, username)) throw new IdentityRejected("INVALID_GAME_IDENTITY");
        if (!existing.exists()) {
            Map<String, Object> fields = new HashMap<>();
            fields.put(USER_ID, uid);
            fields.put(EMAIL, email);
            fields.put(DISPLAY_NAME, displayName);
            tx.create(identity, fields);
        }
        if (reserved != null && !reserved.exists()) {
            Map<String, Object> fields = new HashMap<>();
            fields.put(USER_ID, uid);
            tx.create(name, fields);
        }
        return displayName;
    }

    /** 새 아이디 계정은 표시 이름이 아이디와 같아야 하고, 이관 계정은 저장된 이름만 쓴다. */
    private static boolean isValidDisplayName(String displayName, String username) {
        return displayName != null && !displayName.isEmpty() && displayName.length() <= 32
                && (username == null || username.equals(displayName));
    }

    private static String await(ApiFuture<String> operation) throws IOException, AuthException {
        try { return operation.get(15, TimeUnit.SECONDS); }
        catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            throw new IOException("Identity lookup interrupted", interrupted);
        } catch (TimeoutException unavailable) {
            operation.cancel(false);
            throw new IOException("Identity lookup timed out", unavailable);
        } catch (ExecutionException failed) {
            for (Throwable cause = failed.getCause(); cause != null; cause = cause.getCause()) {
                if (cause instanceof IdentityRejected) throw new AuthException(cause.getMessage());
            }
            throw new IOException("Identity lookup unavailable", failed.getCause());
        }
    }

    private static final class IdentityRejected extends RuntimeException {
        private static final long serialVersionUID = 1L;
        IdentityRejected(String code) { super(code); }
    }
}
