import { useState, useEffect, useCallback } from 'react';
import { useParams, Link, useNavigate } from 'react-router-dom';
import Layout from '../components/Layout';
import {
  ArrowLeft, AlertTriangle, XCircle, Clock,
  TrendingUp, BarChart2, RefreshCw, Eye
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
  const accentMap = { green: 'border-t-green-500', blue: 'border-t-blue-500', red: 'border-t-red-500', amber: 'border-t-amber-500' };
  const textMap   = { green: 'text-slate-900',     blue: 'text-slate-900',    red: 'text-red-700', amber: 'text-amber-700' };
  const labelMap  = { green: 'text-slate-500',     blue: 'text-slate-500',    red: 'text-red-600', amber: 'text-amber-600' };
  return (
    <div className={`bg-white p-6 rounded-xl border border-slate-200 shadow-sm border-t-4 ${accentMap[accent]}`}>
      <p className={`text-[11px] font-bold uppercase tracking-widest ${labelMap[accent]}`}>{label}</p>
      <div className="flex items-end gap-2 mt-3">
        <span className={`text-4xl font-black tabular-nums ${textMap[accent]}`}>{value}</span>
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
    }}>
      <p style={{ color: '#64748b', marginBottom: '4px', fontWeight: 600 }}>{label}</p>
      {payload.map(p => {
        const isResultsMode = mode === 'results' || p.name === 'Passou' || p.name === 'Falhou';
        let displayValue;
        
        if (isResultsMode) {
          displayValue = Math.round(p.value); 
        } else {
          displayValue = typeof p.value === 'number' ? p.value.toFixed(unit === 'ms' ? 0 : 2) : p.value;
          displayValue += (unit ?? '');
        }

        return (
          <p key={p.dataKey || p.name} style={{ color: p.color || p.payload?.fill, margin: 0 }}>
            {p.name}: <span style={{ fontWeight: 700 }}>{displayValue}</span>
          </p>
        );
      })}
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

  // ESTADO CHAVE: Controla se estamos na Visão Global (null) ou Visão de Build (objeto)
  const [selectedExecution, setSelectedExecution] = useState(null);

  const [metrics, setMetrics] = useState({
    projectName: 'A carregar...',
    healthScore: 0,
    totalExecutions: 0,
    totalFlaky: 0,
    lastExecutionTime: '',
    recentFailures: [],
    flakyTests: [],
  });

  const [historyData, setHistoryData] = useState([]);

  const fetchMetrics = useCallback(async (executionId = null) => {
    try {
      const endpoint = executionId 
        ? `api/v1/projects/${id}/executions/${executionId}/metrics`
        : `api/v1/projects/${id}/dashboard/metrics/global`;
      
      const res = await api.get(endpoint);
      setMetrics(res.data);
    } catch (error) {
      toast.error('Erro ao carregar métricas.');
      console.error(error);
    }
  }, [id]);

  const fetchDashboardData = useCallback(async (showRefreshSpinner = false) => {
    try {
      if (showRefreshSpinner) setIsRefreshing(true);
      else setIsLoading(true);

      // 1. Busca o histórico de execuções para o gráfico
      const historyRes = await api.get(`api/v1/projects/${id}/dashboard/history?size=100`);
      
      const content = historyRes.data.content || [];

      const formattedHistory = content.map((exec, idx) => {
        const timeString = new Date(exec.startTime).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
        return {
          name: exec.versionName !== 'N/A' ? `${exec.versionName} (${timeString})` : `Run ${idx + 1}`,
          rawDuration: exec.durationMillis || 0,
          passedCount: exec.passedCount    || 0,
          failedCount: exec.failedCount    || 0,
          id:          exec.id || exec.executionId,
        };
      }).reverse();

      setHistoryData(formattedHistory);

      // 2. Atualiza as métricas (Cards) usando o endpoint correto
      await fetchMetrics(selectedExecution?.id);

    } catch (error) {
      toast.error('Erro ao sincronizar dados com o servidor.');
      console.error(error);
    } finally {
      setIsLoading(false);
      setIsRefreshing(false);
    }
  }, [id, selectedExecution, fetchMetrics]);

  useEffect(() => {
    if (id) fetchDashboardData();
  }, [fetchDashboardData, id]);

  // Quando o utilizador clica no switch, atualizamos apenas as métricas
  const handleToggleView = (execObj) => {
    setSelectedExecution(execObj);
    fetchMetrics(execObj?.id);
  };

  const chartData = historyData.map(item => {
    let value = item.rawDuration;
    if (timeUnit === 's') value /= 1000;
    if (timeUnit === 'm') value /= 60000;
    return { ...item, duration: Number(value.toFixed(timeUnit === 'ms' ? 0 : 2)) };
  });

  // Clica numa barra do gráfico: Muda o contexto em vez de sair da página
  const handleChartClick = useCallback((state) => {
    if (!state?.activeLabel) return;
    const hit = chartData.find(item => item.name === state.activeLabel);
    if (hit?.id) {
      handleToggleView(hit);
    }
  }, [chartData]);

  // Dados para o Gráfico Circular
  const pieData = selectedExecution ? [
    { name: 'Passou', value: selectedExecution.passedCount },
    { name: 'Falhou', value: selectedExecution.failedCount }
  ] : [];
  const PIE_COLORS = ['#86efac', '#fca5a5'];

  return (
    <Layout>
      {/* ── Cabeçalho ── */}
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

        {/* Global x Build */}
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
            <button 
              className="px-4 py-1.5 text-sm font-semibold rounded-lg bg-white text-blue-600 shadow-sm transition-all"
            >
              Build
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
          {/* ── Cards de métricas ── */}
          <div className="grid grid-cols-1 md:grid-cols-3 gap-6 mb-8">
            <MetricCard
              label={selectedExecution ? "Saúde da Build" : "Média de Saúde"} 
              value={`${metrics.healthScore}%`} 
              accent="green"
              sub={metrics.healthScore >= 90 ? 'Estável' : metrics.healthScore >= 70 ? 'Atenção recomendada' : 'Requer ação imediata'}
            />
            <MetricCard 
              label={selectedExecution ? "Total de Testes (Build)" : "Total de Execuções"} 
              value={selectedExecution ? (selectedExecution.passedCount + selectedExecution.failedCount) : metrics.totalExecutions} 
              accent="blue" 
            />
            <MetricCard
              label={selectedExecution ? "Flakys Ativos" : "Flakys Globais"} 
              value={metrics.totalFlaky} 
              accent={selectedExecution ? "amber" : "red"}
              sub={metrics.totalFlaky === 0 ? 'Sem instabilidades' : `${metrics.totalFlaky} teste(s) instáveis detetados`}
            />
          </div>

          {/* ── Gráficos ── */}
          <div className="bg-white rounded-xl border border-slate-200 p-5 mb-8 shadow-sm">
            <div className="flex flex-col sm:flex-row items-start sm:items-center justify-between mb-4 gap-3">
              <div className="flex items-center gap-2">
                <TrendingUp size={15} className="text-blue-500" />
                <h3 className="font-semibold text-slate-700 text-xs uppercase tracking-wider">
                  {selectedExecution ? ` ${selectedExecution.name}` : 'Evolução e Tendências'}
                </h3>
                {!selectedExecution && <span className="hidden sm:inline text-[10px] text-slate-300 ml-1">· clique numa barra para detalhe</span>}
              </div>

              {/* Filtros e Toggles modo global */}
              {!selectedExecution && (
                <div className="flex items-center gap-2">
                  <div className="flex bg-slate-50 border border-slate-200 rounded-lg p-0.5 gap-0.5">
                    {[
                      { key: 'duration', icon: <TrendingUp size={11} />, label: 'Tempo'      },
                      { key: 'results',  icon: <BarChart2  size={11} />, label: 'Resultados' },
                    ].map(({ key, icon, label }) => (
                      <button key={key} onClick={() => setChartMode(key)}
                        className={`flex items-center gap-1 px-2.5 py-1 text-[11px] font-medium rounded-md transition-all ${
                          chartMode === key ? 'bg-white text-blue-600 shadow-sm' : 'text-slate-400 hover:text-slate-600'
                        }`}>
                        {icon}{label}
                      </button>
                    ))}
                  </div>
                </div>
              )}
              
              {/* Botão de ver todos os detalhes na Visão de Build */}
              {selectedExecution && (
                <button 
                  onClick={() => navigate(`/projects/${id}/executions/${selectedExecution.id}`)}
                  className="flex items-center gap-1 px-3 py-1.5 text-xs font-semibold text-blue-600 bg-blue-50 rounded-lg hover:bg-blue-100 transition-colors"
                >
                  <Eye size={14} /> Ver todos os testes
                </button>
              )}
            </div>

            <div className="h-64">
              {/* Se for Visão de Build, mostra Donut Chart. Se for Global, mostra Bar/Area Chart */}
              {selectedExecution ? (
                <ResponsiveContainer width="100%" height="100%">
                  <PieChart>
                    <Pie data={pieData} cx="50%" cy="50%" innerRadius={70} outerRadius={100} paddingAngle={3} dataKey="value">
                      {pieData.map((entry, index) => (
                        <Cell key={`cell-${index}`} fill={PIE_COLORS[index % PIE_COLORS.length]} />
                      ))}
                    </Pie>
                    <Tooltip content={<CustomTooltip mode="results" />} />
                  </PieChart>
                </ResponsiveContainer>
              ) : chartData.length > 0 ? (
                <ResponsiveContainer width="100%" height="100%">
                  {chartMode === 'duration' ? (
                    <AreaChart data={chartData} onClick={handleChartClick} style={{ cursor: 'pointer' }} margin={{ top: 8, right: 8, left: 0, bottom: 0 }}>
                      <CartesianGrid horizontal={true} vertical={false} stroke="#f1f5f9" />
                      <XAxis dataKey="name" tick={{ fill: '#cbd5e1', fontSize: 10 }} axisLine={false} tickLine={false} dy={6} />
                      <YAxis tick={{ fill: '#cbd5e1', fontSize: 10 }} axisLine={false} tickLine={false} unit={timeUnit} width={45} tickCount={4} />
                      <Tooltip content={<CustomTooltip unit={timeUnit} mode={chartMode} />} />
                      <Area type="monotone" dataKey="duration" stroke="#3b82f6" strokeWidth={1.5} fill="#eff6ff" />
                    </AreaChart>
                  ) : (
                    <BarChart data={chartData} onClick={handleChartClick} style={{ cursor: 'pointer' }} margin={{ top: 8, right: 8, left: 0, bottom: 0 }} barCategoryGap="40%" barGap={1}>
                      <CartesianGrid horizontal={true} vertical={false} stroke="#f1f5f9" />
                      <XAxis dataKey="name" tick={{ fill: '#cbd5e1', fontSize: 10 }} axisLine={false} tickLine={false} dy={6} />
                      <YAxis tick={{ fill: '#cbd5e1', fontSize: 10 }} axisLine={false} tickLine={false} width={36} tickCount={4} />
                      <Tooltip content={<CustomTooltip mode={chartMode} />} />
                      <Bar dataKey="passedCount" name="Passou" stackId="a" fill="#86efac" radius={[0, 0, 2, 2]} />
                      <Bar dataKey="failedCount" name="Falhou" stackId="a" fill="#fca5a5" radius={[2, 2, 0, 0]} />
                    </BarChart>
                  )}
                </ResponsiveContainer>
              ) : (
                <div className="flex flex-col items-center justify-center h-full gap-2"><BarChart2 size={24} className="text-slate-200" /><p className="text-xs text-slate-300 italic">Sem dados disponíveis.</p></div>
              )}
            </div>

            {/* Legenda */}
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

          {/* ── Tabelas Inferiores ── */}
          <div className="grid grid-cols-1 lg:grid-cols-2 gap-8">
            {/* Falhas críticas */}
            <div className="bg-white rounded-xl border border-slate-200 shadow-sm overflow-hidden flex flex-col">
              <div className="px-6 py-4 border-b border-slate-100 flex items-center justify-between">
                <div className="flex items-center gap-2">
                  <XCircle size={16} className="text-red-500" />
                  <h3 className="font-bold text-slate-800 text-xs uppercase tracking-wider">
                    {selectedExecution ? "Falhas nesta Build" : "Falhas Críticas (Última Run)"}
                  </h3>
                </div>
              </div>
              <div className="overflow-x-auto flex-1">
                {metrics.recentFailures?.length > 0 ? (
                  <table className="min-w-full divide-y divide-slate-100">
                    <tbody className="divide-y divide-slate-100 text-sm">
                      {metrics.recentFailures.map(test => (
                        <tr key={test.id} className="hover:bg-red-50/40 transition-colors group">
                          <td className="px-6 py-3 text-xs font-mono text-slate-600 break-all group-hover:text-red-700 transition-colors">{test.name}</td>
                          <td className="px-6 py-3 text-right shrink-0">
                            <span className="text-[10px] font-bold uppercase px-2 py-1 bg-red-100 text-red-600 rounded-md">{test.status}</span>
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

            {/* Ranking Flaky */}
            <div className="bg-white rounded-xl border border-slate-200 shadow-sm overflow-hidden flex flex-col">
              <div className="px-6 py-4 border-b border-slate-100 flex items-center justify-between">
                <div className="flex items-center gap-2">
                  <AlertTriangle size={16} className="text-amber-500" />
                  <h3 className="font-bold text-slate-800 text-xs uppercase tracking-wider">
                    {selectedExecution ? "Flakys Ativos (Nesta Build)" : "Testes Instáveis (Global)"}
                  </h3>
                </div>
              </div>
              <div className="overflow-x-auto flex-1">
                {metrics.flakyTests?.length > 0 ? (
                  <table className="min-w-full divide-y divide-slate-100">
                    <tbody className="divide-y divide-slate-100 text-sm">
                      {metrics.flakyTests.map((test, i) => (
                        <tr key={test.id} className="hover:bg-amber-50/40 transition-colors">
                          <td className="px-6 py-3">
                            <div className="flex items-start gap-2">
                              <span className="text-[10px] font-bold text-slate-400 mt-0.5 shrink-0">#{i + 1}</span>
                              <span className="text-xs font-mono text-slate-600 break-all">{test.name}</span>
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