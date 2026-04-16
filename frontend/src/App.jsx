import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { Toaster } from 'react-hot-toast';
import { AuthProvider } from './contexts/AuthContext';
import Login from './pages/Login';
import Dashboard from './pages/Dashboard';
import Register from './pages/Register';
import PrivateRoute from './components/PrivateRoute';
import ProjectDetails from './pages/ProjectDetails';
import ExecutionDetails from './pages/ExecutionDetails';
import BuildHistory from './pages/BuildHistory';


function App() {
  return (
    <AuthProvider>
      <BrowserRouter>
        <Toaster position="top-right" toastOptions={{ duration: 4000, style: { background: '#fff', color: '#0f172a', border: '1px solid #e2e8f0' } }} />
        <Routes>
          <Route path="/" element={<Navigate to="/login" replace />} />
          
          {/* Rotas Públicas */}
          <Route path="/login" element={<Login />} />
          <Route path="/register" element={<Register />} />
          
          {/* Rotas Protegidas*/}
          <Route 
            path="/dashboard" 
            element={
              <PrivateRoute>
                <Dashboard />
              </PrivateRoute>
            } 
          />

          <Route 
            path="/projects/:id" 
            element={
              <PrivateRoute>
                <ProjectDetails />
              </PrivateRoute>
            } 
          />

          <Route 
            path="/projects/:projectId/executions/:executionId" 
            element={
              <PrivateRoute>
                <ExecutionDetails />
              </PrivateRoute>
            } 
          />

          <Route 
            path="/projects/:projectId/build-history" 
            element={
              <PrivateRoute>
                <BuildHistory />
              </PrivateRoute>
            } 
          />
          
          <Route path="*" element={<Navigate to="/login" replace />} />
        </Routes>
      </BrowserRouter>
    </AuthProvider>
  );
}

export default App;