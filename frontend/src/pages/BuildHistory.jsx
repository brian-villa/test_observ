import React, { useState, useEffect } from 'react';
import { useParams, Link, useLocation, useNavigate } from 'react-router-dom';
import Layout from '../components/Layout';
import { ArrowLeft, History, PlayCircle, Clock, ChevronRight } from 'lucide-react';
import api from '../services/api';

export default function BuildHistory() {
  const { projectId } = useParams();
  const location = useLocation();
  const navigate = useNavigate();
  const versionName = location.state?.versionName || 'Build';

  const [runs, setRuns] = useState([]);
  const [isLoading, setIsLoading] = useState(true);

  useEffect(() => {
    const fetchRuns = async () => {
      try {
        setIsLoading(true);
        const res = await api.get(`/api/v1/projects/${projectId}/dashboard/history?versionName=${versionName}&size=50`);
        setRuns(res.data.content || []);
      } catch (error) {
        console.error("Erro ao buscar histórico da build:", error);
      } finally {
        setIsLoading(false);
      }
    };
    fetchRuns();
  }, [projectId, versionName]);

  return (
    <Layout>
      <div className="mb-8">
        <Link to={`/projects/${projectId}`} className="inline-flex items-center text-sm text-slate-500 hover:text-slate-700 mb-4 transition-colors">
          <ArrowLeft size={16} className="mr-1" /> Voltar ao Dashboard
        </Link>
        <div className="flex items-center gap-3">
          <History className="text-blue-500" size={28} />
          <h1 className="text-3xl font-bold text-slate-900">Histórico {versionName}</h1>
        </div>
      </div>

      <div className="bg-white rounded-xl border border-slate-200 shadow-sm overflow-hidden">
        <div className="px-6 py-4 border-b border-slate-100 bg-slate-50/50">
          <h3 className="text-xs font-bold text-slate-500 uppercase tracking-wider">Lista de Execuções</h3>
        </div>

        {runs.length > 0 ? (
          <div className="divide-y divide-slate-100">
            {runs.map((run) => {
              const validRunId = run.id || run.executionId;

              return (
                <div 
                  key={validRunId}
                  onClick={() => navigate(`/projects/${projectId}/executions/${validRunId}`, { state: { buildName: versionName } })}
                  className="group p-6 hover:bg-slate-50 transition-all cursor-pointer flex items-center justify-between"
                >
                  <div className="flex items-center gap-4">
                    <div className="p-3 bg-blue-50 text-blue-600 rounded-full group-hover:bg-blue-600 group-hover:text-white transition-colors">
                      <PlayCircle size={20} />
                    </div>
                    <div>
                      <p className="font-bold text-slate-900">Execução em {new Date(run.startTime).toLocaleString()}</p>
                      <div className="flex items-center gap-4 mt-1">
                        <span className="flex items-center gap-1 text-xs text-slate-500"><Clock size={12}/> {Math.round(run.durationMillis / 1000)}s</span>
                        <span className="text-xs font-semibold text-green-600 bg-green-100 px-2 py-0.5 rounded">{run.passedCount || 0} Pass</span>
                        <span className={`text-xs font-semibold px-2 py-0.5 rounded ${(run.failedCount > 0 || run.hasFailures) ? 'bg-red-100 text-red-600' : 'bg-slate-100 text-slate-400'}`}>
                          {run.failedCount || 0} Fail
                        </span>

                        {run.flakyCount > 0 && (
                          <span className="text-xs font-semibold text-amber-600 bg-amber-100 px-2 py-0.5 rounded">
                            {run.flakyCount} Flaky
                          </span>
                        )}
                        
                      </div>
                    </div>
                  </div>
                  <ChevronRight className="text-slate-300 group-hover:text-blue-500 transition-colors" />
                </div>
              );
            })}
          </div>
        ) : (
          <div className="py-20 text-center text-slate-400 italic">Nenhuma execução encontrada.</div>
        )}
      </div>
    </Layout>
  );
}