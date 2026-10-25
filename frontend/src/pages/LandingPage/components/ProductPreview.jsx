import { PREVIEW } from "../landingData";

function ProductPreview() {
  return (
    <section className="landing-section" id="preview" aria-labelledby="preview-heading">
      <div className="landing-container">
        <div className="landing-section-head">
          <span className="landing-kicker">Product preview</span>
          <h2 className="landing-title" id="preview-heading">
            See the story behind <em>every alert.</em>
          </h2>
          <p className="landing-sub">
            Move beyond isolated alerts with an investigation experience designed around context,
            evidence, and analyst decisions.
          </p>
        </div>

        <div className="landing-preview" role="img" aria-label="Illustrative SentinelCore investigation interface">
          <div className="landing-preview-bar">
            <div className="landing-preview-dots" aria-hidden="true">
              <span />
              <span />
              <span />
            </div>
            <span className="landing-preview-title">SentinelCore — Investigations</span>
            <span className="landing-illustrative">Illustrative investigation</span>
          </div>

          <div className="landing-preview-body">
            <aside className="landing-preview-side" aria-label="Example navigation">
              <ul>
                {PREVIEW.sidebar.map((item) => (
                  <li key={item} className={item === PREVIEW.activeItem ? "active" : undefined} aria-current={item === PREVIEW.activeItem ? "true" : undefined}>
                    {item}
                  </li>
                ))}
              </ul>
            </aside>

            <div className="landing-preview-main">
              <div className="landing-alert-head">
                <h4>{PREVIEW.alertTitle}</h4>
                <span className="landing-chip landing-chip-high">High severity</span>
                <span className="landing-chip landing-chip-status">Investigating</span>
              </div>

              <div className="landing-preview-grid">
                <div className="landing-preview-left">
                  <div className="landing-block">
                    <span className="landing-block-title">Event timeline</span>
                    <ul className="landing-timeline">
                      {PREVIEW.timeline.map((entry) => (
                        <li key={entry.time}>
                          <time>{entry.time}</time>
                          <p>{entry.event}</p>
                        </li>
                      ))}
                    </ul>
                  </div>
                  <div className="landing-block landing-summary">
                    <span className="landing-block-title">Investigation summary</span>
                    <p>{PREVIEW.summary}</p>
                  </div>
                </div>

                <div className="landing-preview-right">
                  <div className="landing-block">
                    <span className="landing-block-title">Evidence</span>
                    <ul className="landing-evidence">
                      {PREVIEW.evidence.map((item) => (
                        <li key={item.label}>
                          <p>{item.text}</p>
                          <span>{item.label}</span>
                        </li>
                      ))}
                    </ul>
                  </div>
                  <div className="landing-block">
                    <span className="landing-block-title">Suggested next checks</span>
                    <ul className="landing-checks">
                      {PREVIEW.nextChecks.map((check) => (
                        <li key={check}>{check}</li>
                      ))}
                    </ul>
                  </div>
                </div>
              </div>

              <p className="landing-preview-note">
                Sample data shown for illustration. No connection to a live security backend is
                represented in this preview.
              </p>
            </div>
          </div>
        </div>
      </div>
    </section>
  );
}

export default ProductPreview;
