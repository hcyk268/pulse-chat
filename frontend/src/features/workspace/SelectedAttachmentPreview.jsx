import { FileText, X } from "lucide-react";
import { useEffect, useState } from "react";
import { attachmentFileKey } from "../../domain/chat/attachmentFiles.js";

function canPreview(file) {
  return file.type.startsWith("image/") || file.type.startsWith("video/");
}

export default function SelectedAttachmentPreview({ disabled, file, fileSize, onRemove, t }) {
  const [previewUrl, setPreviewUrl] = useState("");

  useEffect(() => {
    if (!canPreview(file)) {
      setPreviewUrl("");
      return undefined;
    }

    const objectUrl = URL.createObjectURL(file);
    setPreviewUrl(objectUrl);
    return () => URL.revokeObjectURL(objectUrl);
  }, [file]);

  return (
    <div className="message-composer__file" key={attachmentFileKey(file)}>
      <span className="message-composer__preview">
        {previewUrl && file.type.startsWith("image/") ? (
          <img src={previewUrl} alt="" />
        ) : null}
        {previewUrl && file.type.startsWith("video/") ? (
          <video src={previewUrl} muted preload="metadata" />
        ) : null}
        {!previewUrl ? <FileText size={17} aria-hidden="true" /> : null}
      </span>
      <span className="message-composer__file-copy">
        <strong>{file.name}</strong>
        <small>{fileSize}</small>
      </span>
      <button
        type="button"
        aria-label={t("chat.removeAttachment")}
        disabled={disabled}
        onClick={() => onRemove(file)}
      >
        <X size={14} />
      </button>
    </div>
  );
}
