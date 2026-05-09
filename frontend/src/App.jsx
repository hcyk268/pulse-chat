import AppRouter from "./routes/AppRouter";
import { ChatProvider } from "./hooks/useChatStore";

export default function App() {
  return (
    <ChatProvider>
      <AppRouter />
    </ChatProvider>
  );
}
