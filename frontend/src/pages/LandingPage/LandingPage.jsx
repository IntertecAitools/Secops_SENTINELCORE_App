import "./landing.css";

import FibreArc from "../../components/originkit/ui/hero-49/fibre-arc";

import LandingNavbar from "./components/LandingNavbar";
import HeroSection from "./components/HeroSection";
import SecurityCapabilities from "./components/SecurityCapabilities";
import BenefitsSection from "./components/BenefitsSection";
import WorkflowSection from "./components/WorkflowSection";
import ProductPreview from "./components/ProductPreview";
import IntegrationsSection from "./components/IntegrationsSection";
import FinalCTA from "./components/FinalCTA";
import LandingFooter from "./components/LandingFooter";

function LandingPage() {
  return (
    <div className="landing">
      <div className="landing-bg" aria-hidden="true">
        <FibreArc style={{ width: "100%", height: "100%" }} />
      </div>

      <div className="landing-content">
        <a className="landing-skip-link" href="#main">
          Skip to main content
        </a>

        <LandingNavbar />

        <main id="main">
          <HeroSection />
          <SecurityCapabilities />
          <BenefitsSection />
          <WorkflowSection />
          <ProductPreview />
          <IntegrationsSection />
          <FinalCTA />
        </main>

        <LandingFooter />
      </div>
    </div>
  );
}

export default LandingPage;
