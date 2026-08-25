import { classNames } from "./utils";

export default function AssetAvatar({ asset, size = "md" }) {
  const symbol = asset?.symbol ?? "";
  const style = asset?.color ? { "--asset-color": asset.color } : undefined;

  return (
    <span className={classNames("asset-avatar", `asset-avatar--${size}`)} style={style}>
      {asset?.imageUrl ? (
        <img src={asset.imageUrl} alt="" loading="lazy" />
      ) : (
        (symbol.slice(0, 1) || "?")
      )}
    </span>
  );
}
