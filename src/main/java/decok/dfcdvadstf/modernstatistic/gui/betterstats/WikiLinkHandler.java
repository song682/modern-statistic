package decok.dfcdvadstf.modernstatistic.gui.betterstats;

/**
 * Contract for screens that can host the item right-click popup overlay:
 * provides the wiki-link confirmation flow and screen dimensions.
 * <p>可承载物品右键弹出菜单的界面契约：提供 Wiki 链接确认流程与界面尺寸。</p>
 * <p>
 * The "View on Wiki" action is a PANELED-mode-only feature (BetterStats
 * heritage): only {@code TBetterStatsScreen} implements this interface, and
 * {@code ItemPopupOverlay} offers the wiki action exclusively when constructed
 * with {@code showWikiLink = true}. The TABBED-mode screen ({@code GuiStatics})
 * deliberately does not implement it — its popup only offers pin/unpin.
 * </p>
 * <p>
 * “查看 Wiki”操作仅属 PANELED 模式（BetterStats 传承）：只有
 * {@code TBetterStatsScreen} 实现该接口，且 {@code ItemPopupOverlay} 仅在以
 * {@code showWikiLink = true} 构造时才提供 Wiki 操作。TABBED 模式界面
 * （{@code GuiStatics}）有意不实现它——其弹窗只提供固定/取消固定。
 * </p>
 */
public interface WikiLinkHandler {

    /**
     * Show the "Are you sure you want to open this link?" confirmation dialog
     * before opening a Wiki URL in the browser.
     * <p>在浏览器中打开 Wiki 链接前，显示“是否确认打开该链接？”确认对话框。</p>
     *
     * @param url the wiki URL to open after confirmation / 确认后打开的 Wiki 链接
     */
    void showWikiConfirm(String url);
}
