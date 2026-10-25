import { INTEGRATIONS } from "../landingData";

const IntegrationCard = ({ integration }) => {
  const Icon = integration.icon;

  return (
    <article className="landing-card">
      <span className="landing-card-icon">
        <Icon />
      </span>
      <div className="landing-integration-name">
        <h3>{integration.name}</h3>
        <span className="landing-badge-planned">{integration.status}</span>
      </div>
      <p className="landing-card-desc">{integration.description}</p>
    </article>
  );
};

function IntegrationsSection() {
  return (
    <section className="landing-section" id="integrations" aria-labelledby="integrations-heading">
      <div className="landing-container">
        <div className="landing-section-head">
          <span className="landing-kicker">Integrations</span>
          <h2 className="landing-title" id="integrations-heading">
            Connect your <em>security ecosystem.</em>
          </h2>
          <p className="landing-sub">
            Build a clearer investigation workflow using the security data sources your
            organization has configured.
          </p>
        </div>

        <div className="landing-grid-2" style={{ maxWidth: 980 }}>
          {INTEGRATIONS.map((integration) => (
            <IntegrationCard key={integration.name} integration={integration} />
          ))}
        </div>

        <p className="landing-integrations-note">
          Planned data sources. Connections become available once your organization configures
          them — no connection status or synchronization is represented on this page.
        </p>
      </div>
    </section>
  );
}

export default IntegrationsSection;
