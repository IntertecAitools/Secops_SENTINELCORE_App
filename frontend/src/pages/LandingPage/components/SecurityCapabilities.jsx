import { CAPABILITIES } from "../landingData";

const CapabilityCard = ({ capability }) => {
  const Icon = capability.icon;

  return (
    <article className="landing-card">
      <span className="landing-card-icon">
        <Icon />
      </span>
      <h3>{capability.title}</h3>
      <ul className="landing-caps">
        {capability.capabilities.map((item) => (
          <li key={item}>{item}</li>
        ))}
      </ul>
      <p className="landing-card-desc">{capability.description}</p>
    </article>
  );
};

function SecurityCapabilities() {
  return (
    <section className="landing-section" id="security" aria-labelledby="security-heading">
      <div className="landing-container">
        <div className="landing-section-head">
          <span className="landing-kicker">Security</span>
          <h2 className="landing-title" id="security-heading">
            Four coverage areas. <em>One investigation workspace.</em>
          </h2>
          <p className="landing-sub">
            SentinelCore organizes security signals into four capability groups so every alert can
            be explored with the context it needs.
          </p>
        </div>

        <div className="landing-grid-4">
          {CAPABILITIES.map((capability) => (
            <CapabilityCard key={capability.title} capability={capability} />
          ))}
        </div>
      </div>
    </section>
  );
}

export default SecurityCapabilities;
