import SecurityIcon from "@mui/icons-material/Security";
import { Link } from "react-router-dom";

import { FOOTER_LINKS } from "../landingData";
import { scrollToSection } from "../scroll";

function LandingFooter() {
  const buildLink = (item) => {
    if (item.target) {
      return (
        <a
          key={item.label}
          href={`#${item.target}`}
          onClick={(e) => {
            e.preventDefault();
            scrollToSection(item.target);
          }}
        >
          {item.label}
        </a>
      );
    }
    return (
      <span key={item.label || item} title="Coming soon">
        {item.label || item}
      </span>
    );
  };

  return (
    <footer className="landing-footer">
      <div className="landing-container">
        <div className="landing-footer-top">
          <div className="landing-footer-brand">
            <Link to="/" className="landing-brand" aria-label="SentinelCore home">
              <SecurityIcon className="landing-brand-icon" />
              <span className="landing-brand-name">SentinelCore</span>
            </Link>
            <p>
              Security operations and infrastructure monitoring for teams that need every signal in
              one clear investigation.
            </p>
          </div>

          <nav className="landing-footer-links" aria-label="Footer">
            {FOOTER_LINKS.sections.map((section) => (
              <div key={section.heading} className="landing-footer-group">
                <h4>{section.heading}</h4>
                {section.items.map((item) => buildLink(item))}
              </div>
            ))}
            <div className="landing-footer-group">
              <h4>Company</h4>
              {FOOTER_LINKS.mutedItems.map((item) => buildLink(item))}
            </div>
          </nav>
        </div>

        <div className="landing-footer-bottom">
          <span>© {new Date().getFullYear()} SentinelCore. All rights reserved.</span>
          <span>Built with SentinelCore SecureOps</span>
        </div>
      </div>
    </footer>
  );
}

export default LandingFooter;
