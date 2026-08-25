export default function PageShell({ eyebrow, title, description, children, action }) {
  return (
    <main className="page-shell" id="main">
      <section className="page-heading">
        <div>
          {eyebrow ? <span className="eyebrow">{eyebrow}</span> : null}
          <h1>{title}</h1>
          {description ? <p>{description}</p> : null}
        </div>
        {action}
      </section>
      {children}
    </main>
  );
}
