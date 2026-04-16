import { useContext } from 'react';
import { AuthContext } from '../contexts/AuthContext';
import { Activity, LogOut, LayoutDashboard } from 'lucide-react';

export default function Layout({ children }) {
  const { logout } = useContext(AuthContext);

  return (
    <div className="min-h-screen bg-slate-50">
      {/* Navegação Superior */}
      <nav className="bg-white border-b border-slate-200 shadow-sm">
        <div className="mx-auto max-w-7xl px-4 sm:px-6 lg:px-8">
          <div className="flex h-16 justify-between items-center">
            
            {/* Lado Esquerdo: Logótipo e Menu */}
            <div className="flex items-center gap-8">
              <div className="flex items-center gap-2">
                <div className="w-8 h-8 bg-primary-600 rounded-md flex items-center justify-center shadow-sm">
                  <Activity className="text-white" size={18} />
                </div>
                <span className="text-xl font-bold text-slate-900 tracking-tight">SGMTA</span>
              </div>
              
              <div className="hidden md:flex space-x-1">
                
              </div>
            </div>

            {/* Lado Direito: Perfil / Logout */}
            <div className="flex items-center">
              <button
                onClick={logout}
                className="flex items-center gap-2 text-sm font-medium text-slate-500 hover:text-slate-700 transition-colors px-3 py-2 rounded-md hover:bg-slate-50"
              >
                <LogOut size={16} />
                <span>Terminar Sessão</span>
              </button>
            </div>
            
          </div>
        </div>
      </nav>

      {/* Área Central onde as páginas serão injetadas */}
      <main className="mx-auto max-w-7xl px-4 sm:px-6 lg:px-8 py-8">
        {children}
      </main>
    </div>
  );
}