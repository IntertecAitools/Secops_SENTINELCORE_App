import { Link } from "react-router-dom";

import { scrollToSection } from "../scroll";

function FinalCTA() {
  const handleScroll = (e, target) => {
    e.preventDefault();
    scrollToSection(target);
  };

  return (
    <section className="landing-section" id="get-started" aria-labelledby="cta-heading">
      <div className="landing-container landing-cta">
        <span className="landing-kicker">Get started</span>
        <h2 className="landing-title" id="cta-heading">
          Bring clarity to your <em>next investigation.</em>
        </h2>
        <p className="landing-sub">
          Explore a security operations experience designed to connect evidence, investigation
          context, and informed decisions.
        </p>

        <div className="landing-cta-buttons">
          <Link to="/login" className="landing-btn landing-btn-primary">
            Get Started
          </Link>
          <a href="#security" onClick={(e) => handleScroll(e, "security")} className="landing-btn landing-btn-ghost">
            Explore the Platform
          </a>
        </div>
      </div>
    </section>
  );
}

export default FinalCTA;
