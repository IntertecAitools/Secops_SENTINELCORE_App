import { BENEFITS } from "../landingData";

const BenefitCard = ({ benefit }) => {
  const Icon = benefit.icon;

  return (
    <article className="landing-card">
      <span className="landing-card-icon">
        <Icon />
      </span>
      <h3>{benefit.title}</h3>
      <ul className="landing-benefit-points">
        {benefit.points.map((point) => (
          <li key={point}>{point}</li>
        ))}
      </ul>
    </article>
  );
};

function BenefitsSection() {
  return (
    <section className="landing-section" id="why" aria-labelledby="why-heading">
      <div className="landing-container">
        <div className="landing-section-head">
          <span className="landing-kicker">Why SentinelCore</span>
          <h2 className="landing-title" id="why-heading">
            From alert overload to <em>confident action.</em>
          </h2>
          <p className="landing-sub">
            SentinelCore brings security signals and investigation context together to help security
            teams understand what matters and decide what to investigate next.
          </p>
        </div>

        <div className="landing-grid-2">
          {BENEFITS.map((benefit) => (
            <BenefitCard key={benefit.title} benefit={benefit} />
          ))}
        </div>
      </div>
    </section>
  );
}

export default BenefitsSection;
