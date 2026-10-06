package kr.ac.jbnu.se.tetris.audio;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;

/** 차용한 세 효과음이 실제 Java 8 PCM 리소스로 읽히고 설정이 보존되는지 확인한다. */
public final class AudioServiceTest {
    private AudioServiceTest() { }
    public static void main(String[] args) throws Exception {
        for (String name : new String[] { "click", "nextlog", "weekSummary" }) {
            try (InputStream source = AudioServiceTest.class.getResourceAsStream(
                        "/audio/university/" + name + ".wav")) {
                check(source != null, name + " resource missing");
                try (AudioInputStream stream = AudioSystem.getAudioInputStream(
                        new BufferedInputStream(source))) {
                    check(stream.getFormat().getSampleRate() > 0, name + " sample rate");
                    check(stream.getFrameLength() > 0, name + " frames");
                    check(stream.getFormat().getChannels() > 0, name + " channels");
                }
            }
        }
        Path folder = Files.createTempDirectory("tetris-audio-test-");
        Path settings = folder.resolve("sound.properties");
        AudioService service = new AudioService(settings);
        service.setMuted(true);
        service.setVolume(0.42f);
        service.play(AudioService.Event.VICTORY);
        check(service.isMuted(), "mute state");
        check(Math.abs(service.getVolume() - .42f) < .001f, "volume state");
        service.close();
        long deadline = System.currentTimeMillis() + 5000;
        while (!Files.isRegularFile(settings) && System.currentTimeMillis() < deadline)
            Thread.sleep(20);
        check(Files.isRegularFile(settings), "settings were not saved");
        AudioService restored = new AudioService(settings);
        check(restored.isMuted(), "saved mute state");
        check(Math.abs(restored.getVolume() - .42f) < .001f, "saved volume");
        restored.close();
        System.out.println("PASS AudioServiceTest");
    }
    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
