import { STAGES } from "../landingData";

function WorkflowSection() {
  return (
    <section className="landing-section" id="how" aria-labelledby="how-heading">
      <div className="landing-container">
        <div className="landing-section-head">
          <span className="landing-kicker">How it works</span>
          <h2 className="landing-title" id="how-heading">
            One workflow. <em>Clearer investigations.</em>
          </h2>
          <p className="landing-sub">
            Every investigation follows the same four stages, from collection through to a recorded
            analyst decision.
          </p>
        </div>

        <ol className="landing-workflow">
          {STAGES.map((stage, index) => (
            <li key={stage.title} className="landing-stage">
              <span className="landing-step" aria-hidden="true">
                {index + 1}
              </span>
              <div className="landing-stage-body">
                <span className="landing-stage-label">{stage.label}</span>
                <h3>{stage.title}</h3>
                <p>{stage.description}</p>
              </div>
            </li>
          ))}
        </ol>
      </div>
    </section>
  );
}

export default WorkflowSection;
