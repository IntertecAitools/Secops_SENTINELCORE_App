"use client";
import FibreArc from "./fibre-arc";

export function SectionHero() {
  return (
    <div className="hero-49">
      <>
        <div id="fibre">
          <FibreArc
            background="#01030A"
            baseColor="#1B4FD8"
            accentColor="#6FC8FF"
            highlight="#FFFFFF"
            density={26}
            speed={100}
            direction={0}
            hover={200}
            reach={23}
            bundle={{ curve: 20, spread: 100, thickness: 100, comb: 170 }}
            style={{ width: "100%", height: "100%" }}
          />
        </div>
        <div className="scrim" />
        <header className="nav">
          <div className="brand">SentinelCore</div>
          <nav className="links">
            <a href="#features">Platform</a>
            <a href="#security">Security</a>
            <a href="#enterprise">Enterprise</a>
            <a href="#pricing">Pricing</a>
          </nav>
          <a className="cta" href="/login">
            Get Started
          </a>
        </header>
        <main className="hero">
          <span className="eyebrow">
            <b>New</b>Enterprise Security Operations Platform
          </span>
          <h1>
            Real-time threat detection,{" "}
            <em>woven into one thread.</em>
          </h1>
          <p className="sub">
            SentinelCore protects organizational infrastructure with audit logging, threat
            intelligence tracking, live vulnerability management, and real-time incident responses.
          </p>
          <div className="buttons">
            <a className="btn" href="/login">
              Start Free Trial
              <svg
                width="16"
                height="16"
                viewBox="0 0 16 16"
                fill="none"
                stroke="currentColor"
                strokeWidth="1.8"
              >
                <path d="M3 8h10M9 4l4 4-4 4" />
              </svg>
            </a>
            <a className="link" href="#features">
              <i>
                <svg width="10" height="12" viewBox="0 0 10 12" fill="#6FC8FF">
                  <path d="M0 0l10 6-10 6z" />
                </svg>
              </i>
              Watch platform overview
            </a>
          </div>
          <div className="sources">
            <span>Protects</span>
            <ul>
              <li>Assets</li>
              <li>Alerts</li>
              <li>Incidents</li>
              <li>Vulnerabilities</li>
              <li>Risk Assessments</li>
              <li>Patches</li>
            </ul>
          </div>
        </main>
      </>
    </div>
  );
}
