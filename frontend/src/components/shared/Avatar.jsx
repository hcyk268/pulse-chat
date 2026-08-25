import { classNames, identityHue, initials } from "./utils";

/**
 * Initials on a colour derived from the identity, so people are recognisable by
 * hue. `seed` should be the most stable identifier available (id or username).
 */
export default function Avatar({ className, name, seed, size = "md", src }) {
  const style = { "--avatar-h": identityHue(seed ?? name) };
  const avatarClassName = classNames("avatar", size !== "md" && `avatar--${size}`, className);

  if (src) {
    return <img className={avatarClassName} src={src} alt="" style={style} />;
  }

  return (
    <span className={avatarClassName} style={style}>
      {initials(name)}
    </span>
  );
}
