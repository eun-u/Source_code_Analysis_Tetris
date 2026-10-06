package kr.ac.jbnu.se.tetris.audio;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;

/** 차용한 배경음·효과음의 PCM 형식과 두 채널 설정을 확인한다. */
public final class AudioServiceTest {
    private AudioServiceTest() { }
    public static void main(String[] args) throws Exception {
        for (String name : new String[] { "click", "nextlog", "weekSummary", "background" }) {
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
        service.startBgm();
        service.setBgmMuted(true);
        service.setBgmVolume(0.23f);
        service.play(AudioService.Event.VICTORY);
        check(service.isMuted(), "mute state");
        check(Math.abs(service.getVolume() - .42f) < .001f, "volume state");
        check(service.isBgmMuted(), "BGM mute state");
        check(Math.abs(service.getBgmVolume() - .23f) < .001f, "BGM volume state");
        service.stopBgm();
        service.close();
        check(Files.isRegularFile(settings) && savedSettingsReady(settings),
                "settings must be saved before close returns");
        AudioService restored = new AudioService(settings);
        check(restored.isMuted(), "saved mute state");
        check(Math.abs(restored.getVolume() - .42f) < .001f, "saved volume");
        check(restored.isBgmMuted(), "saved BGM mute state");
        check(Math.abs(restored.getBgmVolume() - .23f) < .001f, "saved BGM volume");
        restored.close();
        System.out.println("PASS AudioServiceTest");
    }
    private static boolean savedSettingsReady(Path settings) {
        java.util.Properties properties = new java.util.Properties();
        try (InputStream stream = Files.newInputStream(settings)) {
            properties.load(stream);
            return "true".equals(properties.getProperty("muted")) &&
                    "0.42".equals(properties.getProperty("volume")) &&
                    "true".equals(properties.getProperty("bgmMuted")) &&
                    "0.23".equals(properties.getProperty("bgmVolume"));
        } catch (Exception ignored) { return false; }
    }
    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
