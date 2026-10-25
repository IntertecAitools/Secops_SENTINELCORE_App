import { COVERAGE_CHIPS } from "../landingData";
import { scrollToSection } from "../scroll";

function HeroSection() {
  const handleScroll = (e, target) => {
    e.preventDefault();
    scrollToSection(target);
  };

  return (
    <section className="landing-hero" id="hero" aria-labelledby="hero-heading">
      <div className="landing-container">
        <div className="landing-hero-inner page-fade-in">
          <span className="landing-eyebrow">Enterprise Security Operations Platform</span>

          <h1 id="hero-heading">
            Every signal.
            <br />
            <em>One clear investigation.</em>
          </h1>

          <p className="landing-hero-sub">
            Connect security signals, understand what matters, and guide investigations from one
            workspace.
          </p>

          <div className="landing-hero-buttons">
            <a href="#security" onClick={(e) => handleScroll(e, "security")} className="landing-btn landing-btn-primary">
              Explore the Platform
              <svg width="16" height="16" viewBox="0 0 16 16" fill="none" stroke="currentColor" strokeWidth="1.8" aria-hidden="true">
                <path d="M3 8h10M9 4l4 4-4 4" />
              </svg>
            </a>
            <a href="#preview" onClick={(e) => handleScroll(e, "preview")} className="landing-btn landing-btn-ghost">
              <svg width="10" height="12" viewBox="0 0 10 12" fill="#6FC8FF" aria-hidden="true">
                <path d="M0 0l10 6-10 6z" />
              </svg>
              Watch Overview
            </a>
          </div>

          <div className="landing-hero-trusted">
            <span>Security coverage</span>
            <ul>
              {COVERAGE_CHIPS.map((chip) => (
                <li key={chip}>{chip}</li>
              ))}
            </ul>
          </div>
        </div>
      </div>
    </section>
  );
}

export default HeroSection;
