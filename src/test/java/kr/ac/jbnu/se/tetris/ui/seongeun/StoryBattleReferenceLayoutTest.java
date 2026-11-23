package kr.ac.jbnu.se.tetris.ui.seongeun;

import java.awt.Component;
import java.awt.Rectangle;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import kr.ac.jbnu.se.tetris.story.MonsterTier;
import kr.ac.jbnu.se.tetris.app.BackgroundSkin;
import kr.ac.jbnu.se.tetris.ui.seongeun.components.BackgroundSkinAssets;
import kr.ac.jbnu.se.tetris.ui.seongeun.components.PixelArena;
import kr.ac.jbnu.se.tetris.ui.seongeun.panels.BattlePanel;

/** Guards the story reference layout without changing the PvP two-board screen. */
public final class StoryBattleReferenceLayoutTest {
    private StoryBattleReferenceLayoutTest() { }

    public static void main(String[] args) throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            BattlePanel panel = new BattlePanel();
            try {
                panel.setEncounter("university", 9, MonsterTier.BOSS, "university:2");
                require(BackgroundSkinAssets.image(BackgroundSkin.WINTER, "university") != null,
                        "The existing winter campus background must remain available");
                assertStory(panel, 1474, 1067);
                assertStory(panel, 1160, 780);
                assertStory(panel, 820, 650);

                panel.setMode(true);
                panel.setSize(1160, 780);
                panel.doLayout();
                Rectangle player = boundsInPanel(panel.getPlayerBoard(), panel);
                Rectangle enemy = boundsInPanel(panel.getEnemyBoard(), panel);
                require(player.width == enemy.width && player.height == enemy.height,
                        "Online battle boards must remain equal size");
                require(!player.intersects(enemy), "Online battle boards intersect");
            } catch (Exception e) { throw new RuntimeException(e); }
            finally { panel.removeAll(); }
        });
    }

    private static void assertStory(BattlePanel panel, int width, int height) throws Exception {
        panel.setSize(width, height);
        panel.doLayout();
        Rectangle player = boundsInPanel(panel.getPlayerBoard(), panel);
        Rectangle enemy = boundsInPanel(panel.getEnemyBoard(), panel);
        Rectangle hold = ((JPanel) field(panel, "pveHoldPanel")).getBounds();
        Rectangle next = ((JPanel) field(panel, "pveNextPanel")).getBounds();
        require(panel.getEnemyBoard().isVisible() && panel.getEnemyBoard().getParent().isVisible(),
                "Story enemy board is hidden");
        require(player.width > enemy.width && player.height > enemy.height,
                "Story player board should be larger than opposing board");
        require(hold.x < player.x && player.x < next.x && next.x < enemy.x,
                "Story elements are out of visual order");
        require(!player.intersects(enemy) && !next.intersects(enemy),
                "Story HUD overlaps opponent board");
        require(player.y > 0 && enemy.y > 0 && enemy.x + enemy.width < width,
                "Story boards extend beyond the viewport");

        PixelArena arena = (PixelArena) field(panel, "arena");
        int rightLaneStart = (Integer) field(arena, "storyRightLaneStart");
        require(rightLaneStart >= enemy.x + enemy.width - arena.getX(),
                "Monster lane starts inside the opponent board");
        Class<?> stageType = Class.forName(PixelArena.class.getName() + "$Stage");
        Constructor<?> constructor = stageType.getDeclaredConstructor(PixelArena.class, int.class, int.class);
        constructor.setAccessible(true);
        Object stage = constructor.newInstance(arena, arena.getWidth(), arena.getHeight());
        int spriteX = (Integer) field(stage, "monsterX");
        int spriteSize = (Integer) field(stage, "monsterSize");
        require(spriteX - spriteSize / 2 > rightLaneStart,
                "Monster sprite is hidden behind the opponent board");
        require(spriteX + spriteSize / 2 < arena.getWidth(),
                "Monster sprite is clipped at the right screen edge");
    }

    private static Rectangle boundsInPanel(Component child, BattlePanel panel) {
        return SwingUtilities.convertRectangle(child.getParent(), child.getBounds(), panel);
    }

    private static Object field(Object owner, String name) throws Exception {
        Field field = owner.getClass().getDeclaredField(name);
        field.setAccessible(true);
        return field.get(owner);
    }

    private static void require(boolean condition, String description) {
        if (!condition) throw new AssertionError(description);
    }
}
