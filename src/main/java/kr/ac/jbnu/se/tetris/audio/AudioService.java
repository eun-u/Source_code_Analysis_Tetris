package kr.ac.jbnu.se.tetris.audio;

import java.io.ByteArrayOutputStream;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLongArray;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.FloatControl;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;

/** 대학생 시뮬레이션 효과음을 UI 스레드와 분리해 재생한다. */
public final class AudioService implements AutoCloseable {
    public enum Event {
        BUTTON(Sound.CLICK), MOVE(Sound.CLICK), ROTATE(Sound.CLICK),
        DROP(Sound.NEXT), LINE_CLEAR(Sound.NEXT), ATTACK(Sound.NEXT),
        HIT(Sound.CLICK), ITEM_ACQUIRE(Sound.CLICK), ITEM_USE(Sound.NEXT),
        HEAL(Sound.NEXT), FEVER(Sound.SUMMARY), VICTORY(Sound.SUMMARY),
        DEFEAT(Sound.NEXT);

        private final Sound sound;
        Event(Sound sound) { this.sound = sound; }
    }

    private enum Sound {
        CLICK("click.wav", 4, 50), NEXT("nextlog.wav", 3, 90),
        SUMMARY("weekSummary.wav", 2, 260);
        private final String file;
        private final int voices;
        private final long minIntervalMillis;
        Sound(String file, int voices, long minIntervalMillis) {
            this.file = file;
            this.voices = voices;
            this.minIntervalMillis = minIntervalMillis;
        }
    }

    private final Path settingsPath;
    private final ThreadPoolExecutor worker;
    private final EnumMap<Sound, List<Clip>> clips = new EnumMap<>(Sound.class);
    private final EnumMap<Sound, Integer> voiceIndex = new EnumMap<>(Sound.class);
    private final AtomicLongArray lastPlayed = new AtomicLongArray(Sound.values().length);
    private volatile boolean muted;
    private volatile float volume = 0.65f;
    private volatile boolean available;
    private volatile boolean closed;

    public AudioService() {
        this(Paths.get(System.getProperty("user.home", "."), ".tetris-monster", "audio.properties"));
    }

    /** 테스트나 임베딩 환경에서 설정 파일 경로를 지정한다. */
    public AudioService(Path settingsPath) {
        if (settingsPath == null) throw new IllegalArgumentException("settingsPath");
        this.settingsPath = settingsPath;
        loadSettings();
        worker = new ThreadPoolExecutor(1, 1, 0L, TimeUnit.MILLISECONDS,
                new ArrayBlockingQueue<Runnable>(64), task -> {
                    Thread thread = new Thread(task, "tetris-audio");
                    thread.setDaemon(true);
                    return thread;
                }, new ThreadPoolExecutor.AbortPolicy());
        worker.execute(this::loadClips);
    }

    public boolean isMuted() { return muted; }
    public float getVolume() { return volume; }
    /** 오디오 장치가 없거나 지원되지 않는 환경에서는 false이다. */
    public boolean isAvailable() { return available; }

    public void setMuted(boolean muted) {
        if (closed) return;
        this.muted = muted;
        enqueueSettings();
        if (muted) enqueueAudio(this::stopAll);
    }

    public void setVolume(float volume) {
        if (closed) return;
        if (!Float.isFinite(volume)) throw new IllegalArgumentException("volume");
        this.volume = Math.max(0f, Math.min(1f, volume));
        enqueueSettings();
        enqueueAudio(this::updateAllVolumes);
    }

    /** 호출 즉시 반환한다. 장치가 준비되지 않았거나 큐가 가득 차면 해당 소리만 생략한다. */
    public void play(Event event) {
        if (event == null || muted || closed || volume <= 0f) return;
        Sound sound = event.sound;
        int index = sound.ordinal();
        long now = System.nanoTime();
        long previous = lastPlayed.get(index);
        if (previous != 0 && now - previous <
                TimeUnit.MILLISECONDS.toNanos(sound.minIntervalMillis)) return;
        if (!lastPlayed.compareAndSet(index, previous, now)) return;
        enqueueAudio(() -> playSound(sound));
    }

    private void enqueueAudio(Runnable action) {
        if (!closed) {
            try { worker.execute(action); } catch (RejectedExecutionException ignored) { }
        }
    }

    private void enqueueSettings() {
        if (closed) return;
        // 재생 요청이 몰려도 설정 저장은 버리지 않는다.
        while (!closed) {
            if (worker.getQueue().remainingCapacity() == 0) worker.getQueue().poll();
            try { worker.execute(this::saveSettings); return; }
            catch (RejectedExecutionException ignored) { if (worker.isShutdown()) return; }
        }
    }

    private void loadClips() {
        boolean opened = false;
        for (Sound sound : Sound.values()) {
            try {
                PcmData pcm = loadPcm(sound.file);
                List<Clip> pool = new ArrayList<>();
                for (int i = 0; i < sound.voices; i++) {
                    try {
                        Clip clip = AudioSystem.getClip();
                        clip.open(pcm.format, pcm.bytes, 0, pcm.bytes.length);
                        setClipVolume(clip);
                        pool.add(clip);
                        opened = true;
                    } catch (LineUnavailableException | IllegalArgumentException error) {
                        break;
                    }
                }
                clips.put(sound, pool);
                voiceIndex.put(sound, 0);
            } catch (IOException | UnsupportedAudioFileException | RuntimeException error) {
                clips.put(sound, new ArrayList<Clip>());
            }
        }
        available = opened;
    }

    private static PcmData loadPcm(String file) throws IOException, UnsupportedAudioFileException {
        String path = "/audio/university/" + file;
        InputStream resource = AudioService.class.getResourceAsStream(path);
        if (resource == null) throw new IOException("Missing audio resource: " + path);
        try (InputStream source = new BufferedInputStream(resource);
             AudioInputStream encoded = AudioSystem.getAudioInputStream(source)) {
            AudioFormat original = encoded.getFormat();
            AudioFormat pcmFormat = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED,
                    original.getSampleRate(), 16, original.getChannels(), original.getChannels() * 2,
                    original.getSampleRate(), false);
            try (AudioInputStream pcm = AudioSystem.getAudioInputStream(pcmFormat, encoded);
                 ByteArrayOutputStream bytes = new ByteArrayOutputStream()) {
                byte[] buffer = new byte[8192];
                int count;
                while ((count = pcm.read(buffer)) != -1) bytes.write(buffer, 0, count);
                return new PcmData(pcmFormat, bytes.toByteArray());
            }
        }
    }

    private void playSound(Sound sound) {
        if (muted || closed) return;
        List<Clip> pool = clips.get(sound);
        if (pool == null || pool.isEmpty()) return;
        int start = voiceIndex.get(sound);
        int selected = start;
        for (int i = 0; i < pool.size(); i++) {
            int candidate = (start + i) % pool.size();
            if (!pool.get(candidate).isRunning()) { selected = candidate; break; }
        }
        voiceIndex.put(sound, (selected + 1) % pool.size());
        try {
            Clip clip = pool.get(selected);
            clip.stop();
            clip.setFramePosition(0);
            setClipVolume(clip);
            clip.start();
        } catch (RuntimeException ignored) {
            // 장치가 실행 중 사라져도 전투는 계속 진행한다.
        }
    }

    private void setClipVolume(Clip clip) {
        try {
            if (clip.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
                FloatControl gain = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);
                float decibels = (float) (20.0 * Math.log10(Math.max(0.001f, volume)));
                gain.setValue(Math.max(gain.getMinimum(), Math.min(gain.getMaximum(), decibels)));
            } else if (clip.isControlSupported(FloatControl.Type.VOLUME)) {
                FloatControl control = (FloatControl) clip.getControl(FloatControl.Type.VOLUME);
                control.setValue(control.getMinimum() + volume *
                        (control.getMaximum() - control.getMinimum()));
            }
        } catch (IllegalArgumentException ignored) { }
    }

    private void updateAllVolumes() {
        for (List<Clip> pool : clips.values()) for (Clip clip : pool) setClipVolume(clip);
    }

    private void stopAll() {
        for (List<Clip> pool : clips.values()) for (Clip clip : pool) clip.stop();
    }

    private void loadSettings() {
        if (!Files.isRegularFile(settingsPath)) return;
        Properties values = new Properties();
        try (InputStream stream = Files.newInputStream(settingsPath)) {
            values.load(stream);
            muted = Boolean.parseBoolean(values.getProperty("muted", "false"));
            float stored = Float.parseFloat(values.getProperty("volume", "0.65"));
            if (Float.isFinite(stored)) volume = Math.max(0f, Math.min(1f, stored));
        } catch (IOException | NumberFormatException ignored) { }
    }

    private void saveSettings() {
        Path parent = settingsPath.getParent();
        if (parent == null) return;
        Path temporary = null;
        try {
            Files.createDirectories(parent);
            temporary = Files.createTempFile(parent, "audio-", ".tmp");
            Properties values = new Properties();
            values.setProperty("muted", Boolean.toString(muted));
            values.setProperty("volume", Float.toString(volume));
            try (OutputStream stream = Files.newOutputStream(temporary)) {
                values.store(stream, "Tetris Monster sound settings");
            }
            try {
                Files.move(temporary, settingsPath, StandardCopyOption.ATOMIC_MOVE,
                        StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException unsupported) {
                Files.move(temporary, settingsPath, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException ignored) {
            // 읽기 전용 홈 디렉터리에서도 효과음은 계속 재생한다.
        } finally {
            if (temporary != null) try { Files.deleteIfExists(temporary); }
                    catch (IOException ignored) { }
        }
    }

    @Override public void close() {
        if (closed) return;
        closed = true;
        worker.getQueue().clear();
        worker.execute(() -> {
            saveSettings();
            for (List<Clip> pool : clips.values()) for (Clip clip : pool) clip.close();
            clips.clear();
            available = false;
        });
        worker.shutdown();
    }

    private static final class PcmData {
        final AudioFormat format;
        final byte[] bytes;
        PcmData(AudioFormat format, byte[] bytes) {
            this.format = format;
            this.bytes = bytes;
        }
    }
}
