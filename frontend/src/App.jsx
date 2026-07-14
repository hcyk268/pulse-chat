import AppRouter from "./routes/AppRouter";
import { AppSettingsProvider } from "./hooks/useAppSettings";
import { ChatProvider } from "./hooks/useChatStore";

export default function App() {
  return (
    <AppSettingsProvider>
      <ChatProvider>
        <AppRouter />
      </ChatProvider>
    </AppSettingsProvider>
  );
}
