import { useState } from 'react';
import { Link } from 'react-router-dom';
import { Key, Activity, Calendar, Eye, EyeOff, Copy } from 'lucide-react';
import toast from 'react-hot-toast';

export default function ProjectCard({ project }) {
  const [showToken, setShowToken] = useState(false);

  const formatDate = (dateString) => {
    if (!dateString) return 'Data desconhecida';
    return new Date(dateString).toLocaleDateString('pt-PT', { 
      year: 'numeric', month: 'short', day: 'numeric' 
    });
  };

  const handleCopy = (e) => {
    e.preventDefault();
    e.stopPropagation();
    navigator.clipboard.writeText(project.projectToken);
    toast.success('API Key copiada!');
  };

  const toggleToken = (e) => {
    e.preventDefault();
    e.stopPropagation();
    setShowToken(!showToken);
  };

  return (
    <Link to={`/projects/${project.id}`} className="bg-white overflow-hidden rounded-xl shadow-sm border border-slate-200 hover:border-primary-400 hover:shadow-md transition-all group cursor-pointer block flex flex-col h-full">
      <div className="p-6 flex-1 flex flex-col">
        
        <div className="flex items-center justify-between mb-4">
          <div className="w-10 h-10 bg-primary-50 rounded-lg flex items-center justify-center group-hover:bg-primary-100 transition-colors">
            <Activity className="text-primary-600" size={20} />
          </div>
          <span className="inline-flex items-center rounded-md bg-green-50 px-2 py-1 text-xs font-medium text-green-700 ring-1 ring-inset ring-green-600/20">
            Ativo
          </span>
        </div>
        
        <h3 className="text-lg font-semibold text-slate-900 truncate group-hover:text-primary-600 transition-colors">{project.name}</h3>
        
        <p className="mt-1 text-sm text-slate-500 line-clamp-2 min-h-[2.5rem] flex-1">
          {project.description || 'Sem descrição.'}
        </p>

        <div className="mt-5 pt-4 border-t border-slate-100">
          <div className="flex justify-between items-center mb-2">
            <p className="text-xs font-medium text-slate-500 uppercase tracking-wider flex items-center gap-1">
              <Key size={12} /> API Key
            </p>
            <div className="flex gap-3 relative z-10">
              <button onClick={toggleToken} className="text-slate-400 hover:text-primary-600 transition-colors" title="Mostrar chave">
                {showToken ? <EyeOff size={14} /> : <Eye size={14} />}
              </button>
              <button onClick={handleCopy} className="text-slate-400 hover:text-primary-600 transition-colors" title="Copiar chave">
                <Copy size={14} />
              </button>
            </div>
          </div>
          <code className="block w-full bg-slate-50 p-2 rounded text-xs text-slate-700 font-mono border border-slate-200 truncate">
            {showToken ? (project.projectToken || 'Sem chave') : '••••••••••••••••••••••••••'}
          </code>
          
          <div className="flex items-center gap-1 text-xs text-slate-400 mt-3">
            <Calendar size={12} />
            <span>Criado a {formatDate(project.createdAt)}</span>
          </div>
        </div>

      </div>
    </Link>
  );
}