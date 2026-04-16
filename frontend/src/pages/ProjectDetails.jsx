import { useState, useEffect, useCallback, useMemo } from 'react';
import { useParams, Link, useNavigate } from 'react-router-dom';
import Layout from '../components/Layout';
import {
  ArrowLeft, AlertTriangle, XCircle, Clock,
  TrendingUp, BarChart2, RefreshCw, Eye, GitCommit
} from 'lucide-react';
import api from '../services/api';
import toast from 'react-hot-toast';
import {
  XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer,
  AreaChart, Area, BarChart, Bar, PieChart, Pie, Cell
} from 'recharts';

function SkeletonCard() {
  return (
    <div className="bg-white rounded-xl border border-slate-200 p-6 animate-pulse">
      <div className="h-3 w-24 bg-slate-200 rounded mb-4" />
      <div className="h-10 w-16 bg-slate-200 rounded" />
    </div>
  );
}

function MetricCard({ label, value, accent, sub }) {
  // Mapeamento de cores atualizado: green, amber, red e o novo slate (neutro)
  const accentMap = { 
    green: 'border-t-green-500', 
    blue: 'border-t-blue-500', 
    red: 'border-t-red-500', 
    amber: 'border-t-amber-500',
    slate: 'border-t-slate-400' 
  };
  const textMap   = { 
    green: 'text-slate-900', 
    blue: 'text-slate-900', 
    red: 'text-red-700', 
    amber: 'text-amber-700',
    slate: 'text-slate-900'
  };
  const labelMap  = { 
    green: 'text-slate-500', 
    blue: 'text-slate-500', 
    red: 'text-red-600', 
    amber: 'text-amber-600',
    slate: 'text-slate-500'
  };

  return (
    <div className={`bg-white p-6 rounded-xl border border-slate-200 shadow-sm border-t-4 ${accentMap[accent] || accentMap.slate}`}>
      <p className={`text-[11px] font-bold uppercase tracking-widest ${labelMap[accent] || labelMap.slate}`}>{label}</p>
      <div className="flex items-end gap-2 mt-3">
        <span className={`text-4xl font-black tabular-nums ${textMap[accent] || textMap.slate}`}>{value}</span>
      </div>
      {sub && <p className="text-xs text-slate-400 mt-1">{sub}</p>}
    </div>
  );
}

function CustomTooltip({ active, payload, label, unit, mode }) {
  if (!active || !payload?.length) return null;
  return (
    <div style={{
      background: 'white', border: '1px solid #e2e8f0', borderRadius: '8px',
      padding: '8px 12px', fontSize: '12px',
      boxShadow: '0 4px 12px rgba(0,0,0,0.08)', minWidth: '120px',
      zIndex: 100
    }}>
      <p style={{ color: '#64748b', marginBottom: '4px', fontWeight: 600 }}>{label}</p>
      {payload.map(p => {
        const isResultsMode = mode === 'results' || p.name === 'Passou' || p.name === 'Falhou';
        let displayValue = isResultsMode ? Math.round(p.value) : (typeof p.value === 'number' ? p.value.toFixed(unit === 'ms' ? 0 : 2) : p.value) + (unit ?? '');
        return (
          <p key={p.dataKey || p.name} style={{ color: p.color || p.payload?.fill, margin: 0 }}>
            {p.name}: <span style={{ fontWeight: 700 }}>{displayValue}</span>
          </p>
        );
      })}
      {payload[0]?.payload?.isAggregated && (
        <p style={{ color: '#94a3b8', fontSize: '9px', marginTop: '6px', fontStyle: 'italic' }}>
          *Mostra apenas a tentativa mais recente desta versão.
        </p>
      )}
    </div>
  );
}

export default function ProjectDetails() {
  const { id } = useParams();
  const navigate = useNavigate();

  const [isLoading, setIsLoading]       = useState(true);
  const [isRefreshing, setIsRefreshing] = useState(false);
  
  const [timeUnit, setTimeUnit]         = useState('s');
  const [chartMode, setChartMode]       = useState('results');

  const [historyView, setHistoryView]   = useState('evolution'); 

  const [selectedExecution, setSelectedExecution] = useState(null);

  const [metrics, setMetrics] = useState({
    projectName: 'A carregar...',
    healthScore: 0,
    totalExecutions: 0,
    totalFlaky: 0,
    lastExecutionTime: ''
  });

  const [rawHistoryData, setRawHistoryData] = useState([]);
   
  const [failuresList, setFailuresList] = useState([]);
  const [flakyList, setFlakyList] = useState([]);

  // status
  const healthStatus = useMemo(() => {
    const score = metrics.healthScore;
    if (score >= 90) return { accent: 'green', label: 'Estável' };
    if (score >= 70) return { accent: 'amber', label: 'Atenção recomendada' };
    return { accent: 'red', label: 'Requer ação imediata' };
  }, [metrics.healthScore]);

  const normalizeTestName = (fullName) => {
    if (!fullName) return "Teste Desconhecido";
    const parts = fullName.split('.');
    return parts.length > 2 ? parts.slice(-2).join('.') : fullName;
  };

  const fetchLists = async (targetExecutionId) => {
    if (!targetExecutionId) return;
    try {
      const failRes = await api.get(`/executions/${targetExecutionId}/results?status=FAIL&size=20`);
      setFailuresList(failRes.data.content || []);

      const flakyRes = await api.get(`/executions/${targetExecutionId}/results?flakyOnly=true&size=20`);
      setFlakyList(flakyRes.data.content || []);
    } catch (error) {
      console.error('Erro ao buscar as listas de testes:', error);
    }
  };

  const fetchMetrics = useCallback(async (executionId = null, latestExecutionId = null) => {
    try {
      const isGlobal = !executionId;
      const endpoint = isGlobal 
        ? `/api/v1/projects/${id}/dashboard/metrics/global`
        : `/api/v1/projects/${id}/executions/${executionId}/metrics`;
      
      const res = await api.get(endpoint);
      setMetrics(res.data);

      const targetId = executionId || latestExecutionId;
      
      if (targetId) {
        const failRes = await api.get(`/executions/${targetId}/results?status=FAIL&size=20`);
        setFailuresList(failRes.data.content || []);

        if (isGlobal) {
          const flakyRes = await api.get(`/api/v1/projects/${id}/dashboard/flaky`);
          setFlakyList(flakyRes.data || []);
        } else {
          const flakyRes = await api.get(`/executions/${targetId}/results?flakyOnly=true&size=20`);
          setFlakyList(flakyRes.data.content || []);
        }
      }

    } catch (error) {
      console.error(error);
      toast.error('Erro ao carregar métricas.');
    }
  }, [id]);

  const fetchDashboardData = useCallback(async (showRefreshSpinner = false) => {
    try {
      if (showRefreshSpinner) setIsRefreshing(true);
      else setIsLoading(true);

      const historyRes = await api.get(`/api/v1/projects/${id}/dashboard/history?size=100`);
      const content = historyRes.data.content || [];

      setRawHistoryData([...content].reverse());

      const latestId = content.length > 0 ? (content[0].id || content[0].executionId) : null;
      await fetchMetrics(selectedExecution?.id, latestId);

    } catch (error) {
      toast.error('Erro ao sincronizar dados com o servidor.');
    } finally {
      setIsLoading(false);
      setIsRefreshing(false);
    }
  }, [id, selectedExecution, fetchMetrics]);

  useEffect(() => {
    if (id) fetchDashboardData();
  }, [fetchDashboardData, id]);

  const latestRunName = useMemo(() => {
    if (rawHistoryData && rawHistoryData.length > 0) {
      const lastRun = rawHistoryData[rawHistoryData.length - 1];
      return lastRun.versionName && lastRun.versionName !== 'N/A' 
        ? lastRun.versionName 
        : `Run ${lastRun.id?.substring(0,8)}`;
    }
    return 'Build Atual';
  }, [rawHistoryData]);

  const displayChartData = useMemo(() => {
    if (!rawHistoryData || rawHistoryData.length === 0) return [];

    let processedData = [];

    if (historyView === 'evolution') {
      const latestRunsMap = new Map();
      rawHistoryData.forEach((exec) => {
        const buildKey = exec.versionName && exec.versionName !== 'N/A' ? exec.versionName : exec.id;
        const execTime = new Date(exec.startTime).getTime();

        if (!latestRunsMap.has(buildKey)) {
          latestRunsMap.set(buildKey, exec);
        } else {
          const existingExec = latestRunsMap.get(buildKey);
          const existingTime = new Date(existingExec.startTime).getTime();
          if (execTime > existingTime) {
            latestRunsMap.set(buildKey, exec);
          }
        }
      });
      processedData = Array.from(latestRunsMap.values())
        .sort((a, b) => new Date(a.startTime).getTime() - new Date(b.startTime).getTime());
    } else {
      processedData = rawHistoryData;
    }

    return processedData.map((exec, idx) => {
      const timeString = new Date(exec.startTime).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
      const cleanName = exec.versionName && exec.versionName !== 'N/A' ? exec.versionName : `Run ${idx + 1}`;
      
      let duration = exec.durationMillis || 0;
      if (timeUnit === 's') duration /= 1000;
      if (timeUnit === 'm') duration /= 60000;

      return {
        name: historyView === 'evolution' ? cleanName : `${cleanName} (${timeString})`,
        versionName: cleanName,
        duration: Number(duration.toFixed(timeUnit === 'ms' ? 0 : 2)),
        passedCount: exec.passedCount || 0,
        failedCount: exec.failedCount || 0,
        id: exec.id || exec.executionId,
        isAggregated: historyView === 'evolution'
      };
    });
  }, [rawHistoryData, historyView, timeUnit]);

  const handleToggleView = (execObj) => {
    setSelectedExecution(execObj);
    fetchMetrics(execObj?.id, rawHistoryData.length > 0 ? rawHistoryData[rawHistoryData.length - 1].id : null);
  };

  const handleChartClick = useCallback((state) => {
    if (!state?.activeLabel) return;
    const hit = displayChartData.find(item => item.name === state.activeLabel);
    
    if (hit) {
      if (historyView === 'evolution') {
        navigate(`/projects/${id}/build-history`, { 
          state: { versionName: hit.versionName } 
        });
      } else {
        navigate(`/projects/${id}/executions/${hit.id}`, { 
          state: { search: '', status: 'ALL', flakyOnly: false, buildName: hit.versionName } 
        });
      }
    }
  }, [displayChartData, id, navigate, historyView]);

  const pieData = selectedExecution ? [
    { name: 'Passou', value: selectedExecution.passedCount },
    { name: 'Falhou', value: selectedExecution.failedCount }
  ] : [];

  const PIE_COLORS = ['#86efac', '#fca5a5'];

  return (
    <Layout>
      <div className="mb-6 flex flex-col md:flex-row md:items-center justify-between gap-4">
        <div>
          <Link to="/dashboard" className="inline-flex items-center text-sm font-medium text-slate-500 mb-4 hover:text-slate-700 transition-colors">
            <ArrowLeft size={16} className="mr-1" /> Voltar ao Gestor
          </Link>
          <div className="flex items-center gap-3">
            <h1 className="text-3xl font-bold text-slate-900">{metrics.projectName}</h1>
            <button
              onClick={() => fetchDashboardData(true)}
              disabled={isRefreshing}
              className="p-1.5 rounded-lg text-slate-400 hover:text-blue-500 hover:bg-blue-50 transition-colors disabled:opacity-40"
            >
              <RefreshCw size={16} className={isRefreshing ? 'animate-spin' : ''} />
            </button>
          </div>
          {metrics.lastExecutionTime && (
            <p className="text-sm text-slate-400 flex items-center gap-1.5 mt-1">
              <Clock size={13} /> {selectedExecution ? 'Executado:' : 'Última execução:'} {metrics.lastExecutionTime}
            </p>
          )}
        </div>

        <div className="flex bg-slate-100 p-1 rounded-xl shadow-inner w-fit border border-slate-200">
          <button 
            onClick={() => handleToggleView(null)}
            className={`px-4 py-1.5 text-sm font-semibold rounded-lg transition-all ${
              !selectedExecution ? 'bg-white text-blue-600 shadow-sm' : 'text-slate-500 hover:text-slate-700'
            }`}
          >
            Visão Global
          </button>
          {selectedExecution && (
            <button className="px-4 py-1.5 text-sm font-semibold rounded-lg bg-white text-blue-600 shadow-sm transition-all">
              Build Selecionada
            </button>
          )}
        </div>
      </div>

      {isLoading ? (
        <div className="space-y-6">
          <div className="grid grid-cols-1 md:grid-cols-3 gap-6"><SkeletonCard /><SkeletonCard /><SkeletonCard /></div>
          <div className="bg-white rounded-xl border border-slate-200 p-6 h-64 animate-pulse"><div className="h-full bg-slate-100 rounded-lg" /></div>
        </div>
      ) : (
        <>
          <div className="grid grid-cols-1 md:grid-cols-3 gap-6 mb-8">
            <MetricCard
              label={selectedExecution ? "Saúde da Build" : "Média de Saúde"} 
              value={`${metrics.healthScore}%`} 
              accent={healthStatus.accent} // Dinâmico (Verde/Amarelo/Vermelho)
              sub={healthStatus.label}     // Dinâmico (Mensagem condizente)
            />
            <MetricCard 
              label={selectedExecution ? "Total de Testes (Build)" : "Total de Execuções"} 
              value={selectedExecution ? (selectedExecution.passedCount + selectedExecution.failedCount) : metrics.totalExecutions} 
              accent="slate" // Neutro (Removido o azul)
            />
            <MetricCard
              label={selectedExecution ? "Flakys Ativos" : "Flakys"} 
              value={metrics.totalFlaky} 
              accent={healthStatus.accent} // Acompanha a saúde conforme solicitado
              sub={metrics.totalFlaky === 0 ? 'Sem instabilidades' : `${metrics.totalFlaky} instabilidade(s) a resolver`}
            />
          </div>

          <div className="bg-white rounded-xl border border-slate-200 p-5 mb-8 shadow-sm">
            <div className="flex flex-col md:flex-row items-start md:items-center justify-between mb-6 gap-4 border-b border-slate-100 pb-4">
              {!selectedExecution ? (
                <div className="flex bg-slate-50 p-1 rounded-lg border border-slate-200 w-full md:w-auto">
                  <button 
                    onClick={() => setHistoryView('evolution')}
                    className={`flex-1 md:flex-none flex items-center justify-center gap-1.5 px-4 py-1.5 text-xs font-semibold rounded-md transition-all ${
                      historyView === 'evolution' ? 'bg-white text-blue-600 shadow-sm' : 'text-slate-500 hover:text-slate-700'
                    }`}
                  >
                    <TrendingUp size={14} /> Evolução de Versão
                  </button>
                  <button 
                    onClick={() => setHistoryView('timeline')}
                    className={`flex-1 md:flex-none flex items-center justify-center gap-1.5 px-4 py-1.5 text-xs font-semibold rounded-md transition-all ${
                      historyView === 'timeline' ? 'bg-white text-blue-600 shadow-sm' : 'text-slate-500 hover:text-slate-700'
                    }`}
                  >
                    <GitCommit size={14} /> Timeline de Runs
                  </button>
                </div>
              ) : (
                <div className="flex items-center gap-2">
                  <TrendingUp size={15} className="text-blue-500" />
                  <h3 className="font-semibold text-slate-700 text-xs uppercase tracking-wider">
                    {` ${selectedExecution.versionName}`}
                  </h3>
                </div>
              )}

              <div className="flex items-center gap-3 ml-auto">
                {!selectedExecution && (
                  <div className="flex bg-slate-50 border border-slate-200 rounded-lg p-0.5 gap-0.5 hidden sm:flex">
                    {[
                      { key: 'duration', icon: <Clock size={11} />, label: 'Tempo'      },
                      { key: 'results',  icon: <BarChart2  size={11} />, label: 'Resultados' },
                    ].map(({ key, icon, label }) => (
                      <button key={key} onClick={() => setChartMode(key)}
                        className={`flex items-center gap-1 px-2.5 py-1 text-[11px] font-medium rounded-md transition-all ${
                          chartMode === key ? 'bg-white text-slate-700 shadow-sm' : 'text-slate-400 hover:text-slate-600'
                        }`}>
                        {icon}{label}
                      </button>
                    ))}
                  </div>
                )}
                
                {selectedExecution && (
                  <button 
                    onClick={() => navigate(`/projects/${id}/executions/${selectedExecution.id}`, {
                      state: { search: '', status: 'ALL', flakyOnly: false, buildName: selectedExecution.versionName }
                    })}
                    className="flex items-center gap-1 px-3 py-1.5 text-xs font-semibold text-blue-600 bg-blue-50 rounded-lg hover:bg-blue-100 transition-colors"
                  >
                    <Eye size={14} /> Ver todos os testes
                  </button>
                )}
              </div>
            </div>

            <div className="h-64">
              {selectedExecution ? (
                <ResponsiveContainer width="100%" height="100%">
                  <PieChart>
                    <Pie data={pieData} cx="50%" cy="50%" innerRadius={70} outerRadius={100} paddingAngle={3} dataKey="value">
                      {pieData.map((entry, index) => <Cell key={`cell-${index}`} fill={PIE_COLORS[index % PIE_COLORS.length]} />)}
                    </Pie>
                    <Tooltip content={<CustomTooltip mode="results" />} />
                  </PieChart>
                </ResponsiveContainer>
              ) : displayChartData.length > 0 ? (
                <ResponsiveContainer width="100%" height="100%">
                  {chartMode === 'duration' ? (
                    <AreaChart data={displayChartData} onClick={handleChartClick} style={{ cursor: 'pointer' }} margin={{ top: 8, right: 8, left: 0, bottom: 0 }}>
                      <CartesianGrid horizontal={true} vertical={false} stroke="#f1f5f9" />
                      <XAxis dataKey="name" tick={{ fill: '#cbd5e1', fontSize: 10 }} axisLine={false} tickLine={false} dy={6} />
                      <YAxis tick={{ fill: '#cbd5e1', fontSize: 10 }} axisLine={false} tickLine={false} unit={timeUnit} width={45} tickCount={4} />
                      <Tooltip content={<CustomTooltip unit={timeUnit} mode={chartMode} />} cursor={{fill: 'transparent', stroke: '#e2e8f0', strokeWidth: 1, strokeDasharray: '3 3'}} />
                      <Area type="monotone" dataKey="duration" stroke="#3b82f6" strokeWidth={1.5} fill="#eff6ff" />
                    </AreaChart>
                  ) : (
                    <BarChart data={displayChartData} onClick={handleChartClick} style={{ cursor: 'pointer' }} margin={{ top: 8, right: 8, left: 0, bottom: 0 }} barCategoryGap="40%" barGap={1}>
                      <CartesianGrid horizontal={true} vertical={false} stroke="#f1f5f9" />
                      <XAxis dataKey="name" tick={{ fill: '#cbd5e1', fontSize: 10 }} axisLine={false} tickLine={false} dy={6} />
                      <YAxis tick={{ fill: '#cbd5e1', fontSize: 10 }} axisLine={false} tickLine={false} width={36} tickCount={4} />
                      <Tooltip content={<CustomTooltip mode={chartMode} />} cursor={{fill: '#f8fafc'}} />
                      <Bar dataKey="passedCount" name="Passou" stackId="a" fill="#86efac" radius={[0, 0, 2, 2]} />
                      <Bar dataKey="failedCount" name="Falhou" stackId="a" fill="#fca5a5" radius={[2, 2, 0, 0]} />
                    </BarChart>
                  )}
                </ResponsiveContainer>
              ) : (
                <div className="flex flex-col items-center justify-center h-full gap-2"><BarChart2 size={24} className="text-slate-200" /><p className="text-xs text-slate-300 italic">Sem dados disponíveis.</p></div>
              )}
            </div>

            {(chartMode === 'results' || selectedExecution) && (
              <div className="flex items-center gap-4 mt-3 justify-center">
                <span className="flex items-center gap-1.5 text-[10px] font-semibold text-slate-500 uppercase tracking-wide">
                  <span className="w-2.5 h-2.5 rounded-sm bg-green-300 inline-block" /> Passou
                </span>
                <span className="flex items-center gap-1.5 text-[10px] font-semibold text-slate-500 uppercase tracking-wide">
                  <span className="w-2.5 h-2.5 rounded-sm bg-red-300 inline-block" /> Falhou
                </span>
              </div>
            )}
          </div>

          <div className="grid grid-cols-1 lg:grid-cols-2 gap-8">
            <div className="bg-white rounded-xl border border-slate-200 shadow-sm overflow-hidden flex flex-col">
              <div className="px-6 py-4 border-b border-slate-100 flex items-center justify-between">
                <div className="flex items-center gap-2">
                  <XCircle size={16} className="text-red-500" />
                  <h3 className="font-bold text-slate-800 text-xs uppercase tracking-wider">
                    {selectedExecution ? "Falhas nesta Build" : "Falhas Críticas (Última Run)"}
                  </h3>
                </div>
              </div>
              <div className="overflow-y-auto overflow-x-auto max-h-[300px] flex-1 custom-scrollbar">
                {failuresList.length > 0 ? (
                  <table className="min-w-full divide-y divide-slate-100">
                    <tbody className="divide-y divide-slate-100 text-sm">
                      {failuresList.map(test => (
                        <tr 
                          key={test.id} 
                          onClick={() => navigate(`/projects/${id}/executions/${test.testExecutionId}`, {
                            state: { 
                              search: '', 
                              status: 'FAIL', 
                              flakyOnly: false, 
                              buildName: selectedExecution ? selectedExecution.versionName : latestRunName
                            }
                          })}
                          className="hover:bg-red-50/40 transition-colors group cursor-pointer"
                        >
                          <td className="px-6 py-3 text-xs font-mono text-slate-600 break-all group-hover:text-red-700 transition-colors" title={test.testCaseName}>
                            {normalizeTestName(test.testCaseName)}
                          </td>
                          <td className="px-6 py-3 text-right shrink-0">
                            <span className="text-[10px] font-bold uppercase px-2 py-1 bg-red-100 text-red-600 rounded-md">
                              {test.result || "FAIL"}
                            </span>
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                ) : (
                  <div className="flex flex-col items-center justify-center py-12 gap-2 text-slate-400">
                    <p className="text-sm italic">Nenhuma falha a registar.</p>
                  </div>
                )}
              </div>
            </div>

            <div className="bg-white rounded-xl border border-slate-200 shadow-sm overflow-hidden flex flex-col">
              <div className="px-6 py-4 border-b border-slate-100 flex items-center justify-between">
                <div className="flex items-center gap-2">
                  <AlertTriangle size={16} className="text-amber-500" />
                  <h3 className="font-bold text-slate-800 text-xs uppercase tracking-wider">
                    {selectedExecution ? "Flakys Nesta Build" : "Flaky Global"}
                  </h3>
                </div>
              </div>
              <div className="overflow-y-auto overflow-x-auto max-h-[300px] flex-1 custom-scrollbar">
                {flakyList.length > 0 ? (
                  <table className="min-w-full divide-y divide-slate-100">
                    <tbody className="divide-y divide-slate-100 text-sm">
                      {flakyList.map((test) => (
                        <tr 
                          key={test.id} 
                          onClick={() => navigate(`/projects/${id}/executions/${test.testExecutionId}`, {
                            state: { 
                              search: '', 
                              status: 'ALL', 
                              flakyOnly: true, 
                              buildName: selectedExecution ? selectedExecution.versionName : latestRunName
                            }
                          })}
                          className="hover:bg-amber-50/40 transition-colors cursor-pointer group"
                        >
                          <td className="px-6 py-3">
                            <div className="flex items-start gap-2">
                              <span className="text-xs font-mono text-slate-600 break-all group-hover:text-amber-700 transition-colors" title={test.testCaseName}>
                                {normalizeTestName(test.testCaseName)}
                              </span>
                            </div>
                          </td>
                          <td className="px-6 py-3 text-right shrink-0">
                             <span className="text-[10px] font-bold uppercase px-2 py-1 bg-amber-100 text-amber-600 rounded-md">Instável</span>
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                ) : (
                  <div className="flex flex-col items-center justify-center py-12 gap-2 text-slate-400">
                    <p className="text-sm italic">Sem testes instáveis detetados.</p>
                  </div>
                )}
              </div>
            </div>
          </div>
        </>
      )}
    </Layout>
  );
}