import React, { useState, useEffect } from 'react';
import { useParams, Link, useLocation, useNavigate } from 'react-router-dom';
import Layout from '../components/Layout';
import { ArrowLeft, History, PlayCircle, Clock, ChevronRight, GitBranch, Layers } from 'lucide-react';
import api from '../services/api';

export default function BuildHistory() {
  const { projectId } = useParams();
  const location = useLocation();
  const navigate = useNavigate();
  
  // Recebe os filtros do ecrã anterior
  const versionName = location.state?.versionName || '';
  const branchName = location.state?.branchName || '';
  const suiteName = location.state?.suiteName || '';

  const [runs, setRuns] = useState([]);
  const [isLoading, setIsLoading] = useState(true);

  useEffect(() => {
    const fetchRuns = async () => {
      try {
        setIsLoading(true);
        const params = new URLSearchParams({ size: 50 });
        if (versionName) params.append('versionName', versionName);
        if (branchName) params.append('branchName', branchName);
        if (suiteName) params.append('suiteName', suiteName);

        const res = await api.get(`/api/v1/projects/${projectId}/dashboard/history?${params.toString()}`);
        setRuns(res.data.content || []);
      } catch (error) {
        console.error("Erro ao buscar histórico da build:", error);
      } finally {
        setIsLoading(false);
      }
    };
    fetchRuns();
  }, [projectId, versionName, branchName, suiteName]);

  // Cria um subtítulo dinâmico para dar contexto ao utilizador
  const subtitle = [
    branchName ? `Branch: ${branchName}` : '',
    suiteName ? `Suite: ${suiteName}` : ''
  ].filter(Boolean).join(' | ');

  return (
    <Layout>
      <div className="mb-8">
        <Link to={`/projects/${projectId}`} className="inline-flex items-center text-sm font-medium text-slate-500 hover:text-slate-700 mb-4 transition-colors">
          <ArrowLeft size={16} className="mr-1" /> Voltar ao Dashboard
        </Link>
        <div className="flex items-center gap-3 mb-2">
          <History className="text-blue-500" size={28} />
          <h1 className="text-3xl font-bold text-slate-900">
            Histórico {versionName ? `da Versão ${versionName}` : 'Global'}
          </h1>
        </div>
        {subtitle && <p className="text-sm font-medium text-slate-500 flex items-center gap-2 mt-1">{subtitle}</p>}
      </div>

      <div className="bg-white rounded-xl border border-slate-200 shadow-sm overflow-hidden">
        <div className="px-6 py-4 border-b border-slate-100 bg-slate-50/50 flex justify-between items-center">
          <h3 className="text-xs font-bold text-slate-500 uppercase tracking-wider">Lista de Builds (Execuções)</h3>
          <span className="text-xs font-semibold text-slate-400 bg-white px-2 py-1 rounded-md border border-slate-200">{runs.length} Builds encontradas</span>
        </div>

        {isLoading ? (
           <div className="py-20 text-center text-slate-400 animate-pulse">A carregar builds...</div>
        ) : runs.length > 0 ? (
          <div className="divide-y divide-slate-100">
            {runs.map((run) => {
              const validRunId = run.id || run.executionId;
              const displayBuildName = run.buildName || `Build ${validRunId.substring(0,8)}`;

              return (
                <div 
                  key={validRunId}
                  onClick={() => navigate(`/projects/${projectId}/executions/${validRunId}`, { state: { buildName: displayBuildName } })}
                  className="group p-6 hover:bg-slate-50 transition-all cursor-pointer flex flex-col sm:flex-row sm:items-center justify-between gap-4"
                >
                  <div className="flex items-start sm:items-center gap-4">
                    <div className="p-3 bg-blue-50 text-blue-600 rounded-full group-hover:bg-blue-600 group-hover:text-white transition-colors shrink-0">
                      <PlayCircle size={20} />
                    </div>
                    <div>
                      {/* O Protagonista: O Nome da Build */}
                      <p className="text-lg font-bold text-slate-900">{displayBuildName}</p>
                      
                      <div className="flex flex-wrap items-center gap-x-4 gap-y-2 mt-1.5">
                        <span className="flex items-center gap-1 text-xs font-medium text-slate-500">
                          <Clock size={12}/> {new Date(run.startTime).toLocaleString()}
                        </span>
                        {run.branchName && (
                           <span className="flex items-center gap-1 text-[10px] font-bold text-slate-600 bg-slate-100 px-1.5 py-0.5 rounded uppercase">
                             <GitBranch size={10}/> {run.branchName}
                           </span>
                        )}
                        <span className="text-xs font-semibold text-green-700 bg-green-100 px-2 py-0.5 rounded">{run.passedCount || 0} Pass</span>
                        <span className={`text-xs font-semibold px-2 py-0.5 rounded ${(run.failedCount > 0 || run.hasFailures) ? 'bg-red-100 text-red-700' : 'bg-slate-100 text-slate-400'}`}>
                          {run.failedCount || 0} Fail
                        </span>
                        {run.flakyCount > 0 && (
                          <span className="text-xs font-semibold text-amber-700 bg-amber-100 px-2 py-0.5 rounded">
                            {run.flakyCount} Flaky
                          </span>
                        )}
                      </div>
                    </div>
                  </div>
                  <ChevronRight className="text-slate-300 group-hover:text-blue-500 transition-colors hidden sm:block" />
                </div>
              );
            })}
          </div>
        ) : (
          <div className="py-20 text-center text-slate-400 italic">Nenhuma execução encontrada para estes filtros.</div>
        )}
      </div>
    </Layout>
  );
}