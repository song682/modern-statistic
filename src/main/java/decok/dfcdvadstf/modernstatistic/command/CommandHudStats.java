package decok.dfcdvadstf.modernstatistic.command;

import decok.dfcdvadstf.modernstatistic.ModernStatistic;
import decok.dfcdvadstf.modernstatistic.gui.betterstats.overlay.StatsHudOverlay;
import net.minecraft.command.ICommand;
import net.minecraft.command.ICommandSender;
import net.minecraft.stats.StatBase;
import net.minecraft.stats.StatList;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.util.IChatComponent;

import java.util.ArrayList;
import java.util.List;

/**
 * Client-side command to manage the pinned statistics HUD overlay.
 * <p>管理固定统计 HUD 叠加层的客户端命令。</p>
 *
 * <ul>
 *   <li>{@code /hudstats add <statId>} — pin a stat to the HUD / 将统计固定到 HUD</li>
 *   <li>{@code /hudstats remove <statId>} — unpin a stat (HUD must be visible) / 取消固定（需 HUD 可见）</li>
 *   <li>{@code /hudstats list} — list pinned stats / 列出已固定的统计</li>
 *   <li>{@code /hudstats clear} — unpin all stats (HUD must be visible) / 清空（需 HUD 可见）</li>
 *   <li>{@code /hudstats toggle} — toggle HUD overlay visibility / 切换 HUD 叠加层可见性</li>
 * </ul>
 */
public class CommandHudStats implements ICommand {

    /** Sub-command names / 子命令名 */
    private static final String[] SUBCOMMANDS = {"add", "remove", "list", "clear", "toggle"};

    @Override
    public String getCommandName() {
        return "hudstats";
    }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        return "hudstats.usage";
    }

    @Override
    public List getCommandAliases() {
        return new ArrayList();
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) {
        if (args.length < 1) {
            sendMessage(sender, new ChatComponentTranslation(getCommandUsage(sender)));
            return;
        }

        String sub = args[0].toLowerCase();
        if ("toggle".equals(sub)) {
            StatsHudOverlay.INSTANCE.toggle();
            boolean on = StatsHudOverlay.INSTANCE.isOverlayEnabled();
            sendMessage(sender, new ChatComponentTranslation(on ? "hudstats.toggle.on" : "hudstats.toggle.off"));
            return;
        }

        if ("list".equals(sub)) {
            listPinned(sender);
            return;
        }

        if ("clear".equals(sub)) {
            requireHudVisible(sender, () -> {
                ModernStatistic.config.clearPinnedStats();
                sendMessage(sender, new ChatComponentTranslation("hudstats.cleared"));
            });
            return;
        }

        if ("add".equals(sub) || "remove".equals(sub)) {
            if (args.length < 2) {
                sendMessage(sender, new ChatComponentTranslation("hudstats.add.usage"));
                return;
            }
            String statId = args[1];
            StatBase stat = StatList.func_151177_a(statId);
            if (stat == null) {
                sendMessage(sender, new ChatComponentTranslation("hudstats.invalid_stat", statId));
                return;
            }
            if ("add".equals(sub)) {
                ModernStatistic.config.addPinnedStat(statId);
                sendMessage(sender, new ChatComponentTranslation("hudstats.added", statId));
            } else {
                requireHudVisible(sender, () -> {
                    ModernStatistic.config.removePinnedStat(statId);
                    sendMessage(sender, new ChatComponentTranslation("hudstats.removed", statId));
                });
            }
            return;
        }

        sendMessage(sender, new ChatComponentTranslation(getCommandUsage(sender)));
    }

    /**
     * Run the given action only while the HUD overlay is visible; otherwise
     * remind the player to show the HUD first (pinning management is restricted
     * to the visible HUD).
     * <p>仅当 HUD 叠加层可见时执行给定操作；否则提醒玩家先显示 HUD
     * （固定管理被限制在 HUD 可见时）。</p>
     */
    private void requireHudVisible(ICommandSender sender, Runnable action) {
        if (!StatsHudOverlay.INSTANCE.isVisible()) {
            sendMessage(sender, new ChatComponentTranslation("hudstats.hud_hidden"));
            return;
        }
        action.run();
    }

    private void listPinned(ICommandSender sender) {
        List<String> ids = ModernStatistic.config.getPinnedStatIds();
        if (ids.isEmpty()) {
            sendMessage(sender, new ChatComponentTranslation("hudstats.list.empty"));
            return;
        }
        sendMessage(sender, new ChatComponentTranslation("hudstats.list.header"));
        for (String id : ids) {
            StatBase stat = StatList.func_151177_a(id);
            String label = stat != null ? stat.func_150951_e().getUnformattedText() : id;
            sendMessage(sender, new ChatComponentText(" - " + label + " (" + id + ")"));
        }
    }

    private void sendMessage(ICommandSender sender, IChatComponent message) {
        sender.addChatMessage(message);
    }

    @Override
    public boolean canCommandSenderUseCommand(ICommandSender sender) {
        return true;
    }

    @Override
    public List addTabCompletionOptions(ICommandSender sender, String[] args) {
        List<String> completions = new ArrayList<>();
        if (args.length == 1) {
            String prefix = args[0].toLowerCase();
            for (String sub : SUBCOMMANDS) {
                if (sub.startsWith(prefix)) {
                    completions.add(sub);
                }
            }
        } else if (args.length == 2) {
            String prefix = args[1].toLowerCase();
            if ("remove".equalsIgnoreCase(args[0])) {
                // Complete with currently pinned stat IDs / 用当前已固定的统计 ID 补全
                for (String id : ModernStatistic.config.getPinnedStatIds()) {
                    if (id.startsWith(prefix)) {
                        completions.add(id);
                    }
                }
            } else if ("add".equalsIgnoreCase(args[0])) {
                // Complete with all registered stat IDs / 用全部已注册统计 ID 补全
                for (Object obj : StatList.allStats) {
                    if (obj instanceof StatBase) {
                        String id = ((StatBase) obj).statId;
                        if (id.startsWith(prefix)) {
                            completions.add(id);
                        }
                    }
                }
            }
        }
        return completions;
    }

    @Override
    public boolean isUsernameIndex(String[] args, int index) {
        return false;
    }

    @Override
    public int compareTo(Object o) {
        return getCommandName().compareTo(((ICommand) o).getCommandName());
    }
}
