import React, { useState, useEffect } from 'react';
import { useParams, Link, useLocation, useNavigate } from 'react-router-dom';
import Layout from '../components/Layout';
import { ArrowLeft, History, Clock, ChevronRight, GitBranch, Layers, FileSearch, GitMerge } from 'lucide-react';
import api from '../services/api';

export default function BuildHistory() {
  const { projectId } = useParams();
  const location = useLocation();
  const navigate = useNavigate();
  
  const versionName = location.state?.versionName || '';
  const branchName = location.state?.branchName || '';
  const filterSuiteName = location.state?.suiteName || '';

  // Vista global = sem versão seleccionada
  const isGlobalView = !versionName;

  const [runs, setRuns] = useState([]);
  const [isLoading, setIsLoading] = useState(true);

  useEffect(() => {
    const fetchRuns = async () => {
      try {
        setIsLoading(true);
        const params = new URLSearchParams({ size: 150 });
        if (versionName) params.append('versionName', versionName);
        if (branchName) params.append('branchName', branchName);
        if (filterSuiteName) params.append('suiteName', filterSuiteName);

        const res = await api.get(`/api/v1/projects/${projectId}/dashboard/history?${params.toString()}`);
        setRuns(res.data.content || []);
      } catch (error) {
        console.error("Erro ao buscar histórico:", error);
      } finally {
        setIsLoading(false);
      }
    };
    fetchRuns();
  }, [projectId, versionName, branchName, filterSuiteName]);

  const subtitle = [
    branchName ? `Branch: ${branchName}` : '',
    filterSuiteName ? `Suite: ${filterSuiteName}` : ''
  ].filter(Boolean).join(' | ');

  return (
    <Layout>
      <div className="mb-8">
        <Link to={`/projects/${projectId}`} className="inline-flex items-center text-xs font-medium text-slate-400 hover:text-slate-600 mb-4 transition-colors gap-1">
          <ArrowLeft size={14} /> Voltar ao Dashboard
        </Link>
        <div className="flex items-center gap-3 mb-2">
          <History className="text-blue-500" size={24} />
          <h1 className="text-2xl font-bold text-slate-900">
            Lista Técnica {versionName ? `— Versão ${versionName}` : '(Global)'}
          </h1>
        </div>
        {subtitle && <p className="text-xs font-medium text-slate-400 flex items-center gap-2 mt-1">{subtitle}</p>}
      </div>

      <div className="bg-white rounded-xl border border-slate-200 shadow-sm overflow-hidden">
        <div className="px-6 py-4 border-b border-slate-100 bg-slate-50/50 flex justify-between items-center">
          <h3 className="text-[11px] font-bold text-slate-400 uppercase tracking-wider">Histórico Individual de Suites</h3>
          <span className="text-xs font-semibold text-slate-400 bg-white px-2 py-1 rounded-md border border-slate-200">{runs.length} Registos</span>
        </div>

        {isLoading ? (
          <div className="py-20 text-center text-slate-400 animate-pulse text-sm">A extrair dados...</div>
        ) : runs.length > 0 ? (
          <div className="divide-y divide-slate-100">
            {runs.map((run) => {
              const validRunId = run.id || run.executionId;
              const cleanSuite = run.suiteName 
                ? run.suiteName.replace('Backend-', '') 
                : (filterSuiteName ? filterSuiteName.replace('Backend-', '') : 'SUITE');
              const displayBuildName = run.buildName || `Build ${validRunId.substring(0, 8)}`;
              const runVersion = run.versionName && run.versionName !== 'N/A' ? run.versionName : null;

              return (
                <div
                  key={validRunId}
                  onClick={() => navigate(`/projects/${projectId}/executions/${validRunId}`, {
                    state: { executionIds: [validRunId], buildName: `${displayBuildName} [${cleanSuite}]` }
                  })}
                  className="group px-5 py-4 hover:bg-slate-50 transition-all cursor-pointer flex flex-col sm:flex-row sm:items-center justify-between gap-4"
                >
                  <div className="flex items-start sm:items-center gap-4">
                    <div className="p-2.5 bg-blue-50 text-blue-500 rounded-lg group-hover:bg-blue-600 group-hover:text-white transition-colors shrink-0">
                      <FileSearch size={18} />
                    </div>
                    <div>
                      <p className="text-sm font-semibold text-slate-800 group-hover:text-blue-700 transition-colors">
                        {displayBuildName}
                      </p>

                      <div className="flex flex-wrap items-center gap-x-2 gap-y-1.5 mt-1.5">
                        <span className="flex items-center gap-1 text-[11px] text-slate-400">
                          <Clock size={11} /> {new Date(run.startTime).toLocaleString()}
                        </span>

                        {/* TAG DE VERSÃO — apenas na vista global */}
                        {isGlobalView && runVersion && (
                          <span className="flex items-center gap-1 text-[10px] font-bold text-indigo-700 bg-indigo-50 border border-indigo-100 px-1.5 py-0.5 rounded uppercase">
                            <GitMerge size={9} /> {runVersion}
                          </span>
                        )}

                        {run.branchName && (
                          <span className="flex items-center gap-1 text-[10px] font-bold text-slate-600 bg-slate-100 border border-slate-200 px-1.5 py-0.5 rounded uppercase">
                            <GitBranch size={9} /> {run.branchName}
                          </span>
                        )}

                        <span className="flex items-center gap-1 text-[10px] font-bold text-indigo-600 bg-indigo-50 border border-indigo-100 px-1.5 py-0.5 rounded uppercase">
                          <Layers size={9} /> {cleanSuite}
                        </span>

                        <span className="text-[10px] font-semibold text-green-700 bg-green-100 px-1.5 py-0.5 rounded">
                          {run.passedCount || 0} Pass
                        </span>
                        <span className={`text-[10px] font-semibold px-1.5 py-0.5 rounded ${(run.failedCount > 0 || run.hasFailures) ? 'bg-red-100 text-red-700' : 'bg-slate-100 text-slate-400'}`}>
                          {run.failedCount || 0} Fail
                        </span>
                      </div>
                    </div>
                  </div>
                  <ChevronRight size={16} className="text-slate-300 group-hover:text-blue-400 transition-colors hidden sm:block" />
                </div>
              );
            })}
          </div>
        ) : (
          <div className="py-20 text-center text-slate-400 italic text-sm">Nenhum registo encontrado.</div>
        )}
      </div>
    </Layout>
  );
}