import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { Toaster } from 'react-hot-toast';
import { AuthProvider } from './contexts/AuthContext';
import Login from './pages/Login';
import Dashboard from './pages/Dashboard';

function App() {
  return (
    <AuthProvider>
      <BrowserRouter>
        {/* Configuração das notificações visuais */}
        <Toaster 
          position="top-right" 
          toastOptions={{ 
            duration: 4000,
            style: { background: '#fff', color: '#0f172a', border: '1px solid #e2e8f0' }
          }} 
        />
        
        <Routes>
          {/* Se aceder à raiz, vai para o Login */}
          <Route path="/" element={<Navigate to="/login" replace />} />
          
          <Route path="/login" element={<Login />} />
          <Route path="/dashboard" element={<Dashboard />} />
          
          {/* Rota de fallback caso escreva um URL que não existe */}
          <Route path="*" element={<Navigate to="/login" replace />} />
        </Routes>
      </BrowserRouter>
    </AuthProvider>
  );
}

export default App;