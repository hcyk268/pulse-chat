import {
  Activity,
  ArrowRight,
  BarChart3,
  Bot,
  Check,
  LineChart,
  MessageCircle,
  Radio,
  ShieldCheck,
  Sparkles,
  TrendingUp,
  Users,
  Zap,
} from "lucide-react";
import { Link } from "react-router-dom";
import logoUrl from "../assets/trader-hub-logo.png";
import { storeLocale } from "../i18n/index.js";
import { useTranslation } from "../i18n/useTranslation.js";
import { useAppDispatch, useAppSelector } from "../store/hooks.js";
import { selectIsAuthenticated } from "../store/slices/authSlice.js";
import { setLocale } from "../store/slices/uiSlice.js";

const marketRows = [
  { name: "Bitcoin", symbol: "BTC", price: "$64,281.44", change: "+2.45%", tone: "orange" },
  { name: "Ethereum", symbol: "ETH", price: "$3,491.12", change: "+1.08%", tone: "violet" },
  { name: "Solana", symbol: "SOL", price: "$145.88", change: "+5.67%", tone: "mint" },
];

const tickerItems = [
  ["BTC", "$64,281.44", "+2.45%"],
  ["ETH", "$3,491.12", "+1.08%"],
  ["SOL", "$145.88", "+5.67%"],
  ["LINK", "$18.52", "+8.10%"],
  ["TOTAL MARKET", "$2.41T", "+1.84%"],
];

const candleHeights = [38, 52, 44, 67, 58, 78, 71, 90, 82, 96, 88, 100];

export default function LandingPage() {
  const dispatch = useAppDispatch();
  const isAuthenticated = useAppSelector(selectIsAuthenticated);
  const { t, locale } = useTranslation();
  const nextLocale = locale === "vi" ? "en" : "vi";
  const primaryPath = isAuthenticated ? "/market" : "/register";

  function switchLocale() {
    storeLocale(nextLocale);
    dispatch(setLocale(nextLocale));
  }

  return (
    <div className="landing-page">
      <a className="skip-link" href="#main">{t("a11y.skipToContent")}</a>

      <header className="landing-nav">
        <div className="landing-nav__inner">
          <Link className="landing-brand" to="/" aria-label={t("common.appName")}>
            <img src={logoUrl} alt="" width="34" height="34" />
            <span>{t("common.appName")}</span>
          </Link>

          <nav className="landing-nav__links" aria-label={t("nav.primary")}>
            <a href="#platform">{t("landing.nav.platform")}</a>
            <a href="#workflow">{t("landing.nav.workflow")}</a>
            <a href="#community">{t("landing.nav.community")}</a>
          </nav>

          <div className="landing-nav__actions">
            <button type="button" className="landing-locale" onClick={switchLocale} aria-label={t("language.switch")}>
              {nextLocale.toUpperCase()}
            </button>
            {!isAuthenticated ? <Link className="landing-signin" to="/login">{t("common.signIn")}</Link> : null}
            <Link className="landing-button landing-button--small" to={primaryPath}>
              {isAuthenticated ? t("landing.openWorkspace") : t("landing.start")}
              <ArrowRight size={15} />
            </Link>
          </div>
        </div>
      </header>

      <main id="main">
        <section className="landing-hero">
          <div className="landing-hero__halo landing-hero__halo--one" aria-hidden="true" />
          <div className="landing-hero__halo landing-hero__halo--two" aria-hidden="true" />
          <div className="landing-container landing-hero__grid">
            <div className="landing-hero__copy">
              <div className="landing-kicker"><span /><Radio size={14} /> {t("landing.hero.kicker")}</div>
              <h1>
                {t("landing.hero.title")}
                <span>{t("landing.hero.titleAccent")}</span>
              </h1>
              <p>{t("landing.hero.body")}</p>
              <div className="landing-hero__actions">
                <Link className="landing-button" to={primaryPath}>
                  {isAuthenticated ? t("landing.openWorkspace") : t("landing.hero.primary")}
                  <ArrowRight size={17} />
                </Link>
                <Link className="landing-button landing-button--ghost" to="/market">
                  <Activity size={17} /> {t("landing.hero.secondary")}
                </Link>
              </div>
              <div className="landing-proof">
                <div className="landing-proof__avatars" aria-hidden="true">
                  <span>AN</span><span>TK</span><span>LY</span><span>+</span>
                </div>
                <div><strong>{t("landing.proof.title")}</strong><small>{t("landing.proof.body")}</small></div>
              </div>
            </div>

            <div className="landing-console-wrap" aria-label={t("landing.console.label")}>
              <span className="landing-orbit-label landing-orbit-label--top"><i /> {t("landing.console.sentiment")}</span>
              <span className="landing-orbit-label landing-orbit-label--bottom"><Zap size={12} /> {t("landing.console.alert")}</span>
              <div className="landing-console">
                <div className="landing-console__top">
                  <div className="landing-console__brand"><span><Activity size={15} /></span> PULSE / 01</div>
                  <div className="landing-console__live"><i /> {t("landing.console.live")}</div>
                </div>

                <div className="landing-console__chart">
                  <div className="landing-console__asset">
                    <div className="coin-mark coin-mark--btc">{"\u20BF"}</div>
                    <div><strong>Bitcoin</strong><small>BTC / USDT</small></div>
                    <span>+2.45%</span>
                  </div>
                  <div className="landing-console__price"><strong>$64,281.44</strong><small>+$1,537.20</small></div>
                  <svg viewBox="0 0 640 210" role="img" aria-label={t("landing.console.chartLabel")}>
                    <defs>
                      <linearGradient id="landing-chart-fill" x1="0" y1="0" x2="0" y2="1">
                        <stop offset="0" stopColor="#7c5cff" stopOpacity=".38" />
                        <stop offset="1" stopColor="#7c5cff" stopOpacity="0" />
                      </linearGradient>
                    </defs>
                    <path className="landing-chart-fill" d="M0 176 C38 171 55 149 87 153 S135 167 163 136 S205 120 234 130 S277 142 307 104 S356 95 381 106 S427 112 451 79 S493 81 522 56 S567 72 596 40 S620 35 640 22 L640 210 L0 210Z" />
                    <path className="landing-chart-line" d="M0 176 C38 171 55 149 87 153 S135 167 163 136 S205 120 234 130 S277 142 307 104 S356 95 381 106 S427 112 451 79 S493 81 522 56 S567 72 596 40 S620 35 640 22" />
                    <circle cx="640" cy="22" r="5" />
                  </svg>
                  <div className="landing-console__axis"><span>09:00</span><span>12:00</span><span>15:00</span><span>{t("landing.console.now")}</span></div>
                </div>

                <div className="landing-console__bottom">
                  <div className="landing-watchlist">
                    <div className="landing-watchlist__head"><span>{t("landing.console.watchlist")}</span><small>24H</small></div>
                    {marketRows.map((asset) => (
                      <div className="landing-market-row" key={asset.symbol}>
                        <span className={`landing-asset-dot landing-asset-dot--${asset.tone}`}>{asset.symbol[0]}</span>
                        <div><strong>{asset.symbol}</strong><small>{asset.name}</small></div>
                        <strong>{asset.price}</strong><em>{asset.change}</em>
                      </div>
                    ))}
                  </div>
                  <div className="landing-astra-card">
                    <div className="landing-astra-card__head"><span><Sparkles size={15} /></span><strong>Astra</strong><small>AI COPILOT</small></div>
                    <p>{t("landing.console.astra")}</p>
                    <div className="landing-astra-card__signal"><i /><span>{t("landing.console.signal")}</span><strong>82%</strong></div>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </section>

        <section className="landing-ticker" aria-label={t("landing.ticker.label")}>
          <div className="landing-ticker__track">
            {[...tickerItems, ...tickerItems].map(([symbol, price, change], index) => (
              <div key={`${symbol}-${index}`}><strong>{symbol}</strong><span>{price}</span><em>{change}</em><i /></div>
            ))}
          </div>
        </section>

        <section className="landing-intro landing-container" id="platform">
          <div className="landing-section-mark"><span>01</span><i /></div>
          <div className="landing-intro__copy">
            <span className="landing-eyebrow">{t("landing.intro.eyebrow")}</span>
            <h2>{t("landing.intro.title")} <em>{t("landing.intro.accent")}</em></h2>
          </div>
          <div className="landing-intro__side">
            <p>{t("landing.intro.body")}</p>
            <div className="landing-stats">
              <div><strong>24/7</strong><span>{t("landing.stats.market")}</span></div>
              <div><strong>&lt;1s</strong><span>{t("landing.stats.realtime")}</span></div>
              <div><strong>4{"\u00D7"}</strong><span>{t("landing.stats.tools")}</span></div>
            </div>
          </div>
        </section>

        <section className="landing-features landing-container" id="community">
          <div className="landing-feature-card landing-feature-card--market">
            <div className="landing-feature-card__number">01 / MARKET</div>
            <div className="landing-feature-card__copy">
              <span className="landing-icon-box"><TrendingUp size={21} /></span>
              <h3>{t("landing.feature.market.title")}</h3>
              <p>{t("landing.feature.market.body")}</p>
              <Link to="/market">{t("landing.feature.market.action")} <ArrowRight size={15} /></Link>
            </div>
            <div className="landing-candles" aria-hidden="true">
              {candleHeights.map((height, index) => <i key={height + index} style={{ "--candle-height": `${height}%` }} />)}
              <span />
            </div>
          </div>

          <div className="landing-feature-card landing-feature-card--community">
            <div className="landing-feature-card__number">02 / COMMUNITY</div>
            <div className="landing-community-art" aria-hidden="true">
              <span>AN</span><span>KL</span><span>DT</span><span>MN</span>
              <i className="landing-community-line landing-community-line--one" />
              <i className="landing-community-line landing-community-line--two" />
            </div>
            <div className="landing-feature-card__copy">
              <span className="landing-icon-box"><Users size={21} /></span>
              <h3>{t("landing.feature.community.title")}</h3>
              <p>{t("landing.feature.community.body")}</p>
              <Link to="/community">{t("landing.feature.community.action")} <ArrowRight size={15} /></Link>
            </div>
          </div>

          <div className="landing-feature-card landing-feature-card--chat">
            <div className="landing-feature-card__number">03 / LIVE CHAT</div>
            <div className="landing-feature-card__copy">
              <span className="landing-icon-box"><MessageCircle size={21} /></span>
              <h3>{t("landing.feature.chat.title")}</h3>
              <p>{t("landing.feature.chat.body")}</p>
              <Link to="/chat">{t("landing.feature.chat.action")} <ArrowRight size={15} /></Link>
            </div>
            <div className="landing-chat-art" aria-hidden="true">
              <div><span>KL</span><p><strong>BTC breakout confirmed?</strong><small>volume just crossed the 20D average</small></p></div>
              <div><span>AN</span><p><strong>Waiting for the retest.</strong><small>risk first, momentum second</small></p></div>
              <i><Radio size={12} /> 148 online</i>
            </div>
          </div>

          <div className="landing-feature-card landing-feature-card--ai">
            <div className="landing-feature-card__number">04 / ASTRA</div>
            <div className="landing-ai-art" aria-hidden="true"><div><Sparkles size={30} /></div><i /><i /></div>
            <div className="landing-feature-card__copy">
              <span className="landing-icon-box"><Bot size={21} /></span>
              <h3>{t("landing.feature.ai.title")}</h3>
              <p>{t("landing.feature.ai.body")}</p>
              <Link to="/ai">{t("landing.feature.ai.action")} <ArrowRight size={15} /></Link>
            </div>
          </div>
        </section>

        <section className="landing-workflow" id="workflow">
          <div className="landing-container">
            <div className="landing-section-mark landing-section-mark--light"><span>02</span><i /></div>
            <div className="landing-workflow__heading">
              <div><span className="landing-eyebrow">{t("landing.workflow.eyebrow")}</span><h2>{t("landing.workflow.title")}</h2></div>
              <p>{t("landing.workflow.body")}</p>
            </div>
            <div className="landing-workflow__steps">
              <article><span>01</span><div><Radio size={22} /><h3>{t("landing.workflow.signal.title")}</h3><p>{t("landing.workflow.signal.body")}</p></div></article>
              <article><span>02</span><div><BarChart3 size={22} /><h3>{t("landing.workflow.context.title")}</h3><p>{t("landing.workflow.context.body")}</p></div></article>
              <article><span>03</span><div><ShieldCheck size={22} /><h3>{t("landing.workflow.action.title")}</h3><p>{t("landing.workflow.action.body")}</p></div></article>
            </div>
          </div>
        </section>

        <section className="landing-quote landing-container">
          <Sparkles size={24} />
          <blockquote>{"\u201C"}{t("landing.quote.body")}{"\u201D"}</blockquote>
          <div><span>AN</span><p><strong>{t("landing.quote.name")}</strong><small>{t("landing.quote.role")}</small></p></div>
        </section>

        <section className="landing-cta landing-container">
          <div className="landing-cta__glow" aria-hidden="true" />
          <span className="landing-eyebrow">{t("landing.cta.eyebrow")}</span>
          <h2>{t("landing.cta.title")}</h2>
          <p>{t("landing.cta.body")}</p>
          <div>
            <Link className="landing-button landing-button--light" to={primaryPath}>{isAuthenticated ? t("landing.openWorkspace") : t("landing.cta.primary")} <ArrowRight size={17} /></Link>
            <Link className="landing-button landing-button--outline" to="/market"><LineChart size={17} /> {t("landing.cta.secondary")}</Link>
          </div>
          <small><Check size={14} /> {t("landing.cta.note")}</small>
        </section>
      </main>

      <footer className="landing-footer landing-container">
        <div><Link className="landing-brand" to="/"><img src={logoUrl} alt="" width="34" height="34" /><span>{t("common.appName")}</span></Link><p>{t("landing.footer.tagline")}</p></div>
        <nav aria-label={t("nav.footer")}><Link to="/market">{t("nav.market")}</Link><Link to="/community">{t("nav.community")}</Link><Link to="/chat">{t("nav.chat")}</Link><Link to="/ai">Astra</Link></nav>
        <span className="landing-footer__status"><i /> {t("landing.footer.status")}</span>
      </footer>
    </div>
  );
}
