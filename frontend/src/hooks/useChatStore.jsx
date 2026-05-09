import { createContext, useContext } from "react";
import { useChatMock } from "./useChatMock";

const ChatContext = createContext(null);

export function ChatProvider({ children }) {
  const chat = useChatMock();

  return <ChatContext.Provider value={chat}>{children}</ChatContext.Provider>;
}

export function useChatStore() {
  const value = useContext(ChatContext);

  if (!value) {
    throw new Error("useChatStore must be used inside ChatProvider");
  }

  return value;
}
