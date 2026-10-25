import { useState } from "react";
import SecurityIcon from "@mui/icons-material/Security";
import MenuIcon from "@mui/icons-material/Menu";
import CloseIcon from "@mui/icons-material/Close";
import { Link } from "react-router-dom";

import { NAV_LINKS } from "../landingData";
import { scrollToSection } from "../scroll";

function LandingNavbar() {
  const [menuOpen, setMenuOpen] = useState(false);

  const handleSectionClick = (e, target) => {
    e.preventDefault();
    setMenuOpen(false);
    scrollToSection(target);
  };

  return (
    <header className="landing-nav">
      <div className="landing-nav-inner">
        <Link to="/" className="landing-brand" aria-label="SentinelCore home">
          <SecurityIcon className="landing-brand-icon" />
          <span className="landing-brand-name">SentinelCore</span>
        </Link>

        <nav className="landing-links" aria-label="Primary">
          {NAV_LINKS.map((link) => (
            <a key={link.label} href={`#${link.target}`} onClick={(e) => handleSectionClick(e, link.target)}>
              {link.label}
            </a>
          ))}
        </nav>

        <div className="landing-nav-actions">
          <Link to="/login" className="landing-nav-signin">
            Sign In
          </Link>
          <Link to="/login" className="landing-nav-cta">
            Get Started
          </Link>
        </div>

        <button
          type="button"
          className="landing-nav-toggle"
          aria-label={menuOpen ? "Close navigation menu" : "Open navigation menu"}
          aria-expanded={menuOpen}
          onClick={() => setMenuOpen((open) => !open)}
        >
          {menuOpen ? <CloseIcon /> : <MenuIcon />}
        </button>
      </div>

      {menuOpen && (
        <nav className="landing-nav-menu" aria-label="Mobile navigation">
          {NAV_LINKS.map((link) => (
            <a key={link.label} href={`#${link.target}`} onClick={(e) => handleSectionClick(e, link.target)}>
              {link.label}
            </a>
          ))}
          <Link to="/login" onClick={() => setMenuOpen(false)}>
            Sign In
          </Link>
          <Link to="/login" className="landing-menu-cta" onClick={() => setMenuOpen(false)}>
            Get Started
          </Link>
        </nav>
      )}
    </header>
  );
}

export default LandingNavbar;
