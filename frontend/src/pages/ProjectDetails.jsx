import { useState, useEffect, useCallback } from 'react';
import { useParams, Link, useNavigate } from 'react-router-dom';
import Layout from '../components/Layout';
import {
  ArrowLeft, AlertTriangle, XCircle, Clock,
  TrendingUp, Filter, BarChart2, RefreshCw
} from 'lucide-react';
import api from '../services/api';
import toast from 'react-hot-toast';
import {
  XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer,
  AreaChart, Area, BarChart, Bar
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
  const accentMap = { green: 'border-t-green-500', blue: 'border-t-blue-500', red: 'border-t-red-500' };
  const textMap   = { green: 'text-slate-900',     blue: 'text-slate-900',    red: 'text-red-700'    };
  const labelMap  = { green: 'text-slate-500',     blue: 'text-slate-500',    red: 'text-red-600'    };
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

// CORREÇÃO 2: Tooltip inteligente que lida com Inteiros vs Tempos
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
        // Se estivermos no modo de Resultados (barras), forçamos inteiro sem unidade
        const isResultsMode = mode === 'results';
        let displayValue;
        
        if (isResultsMode) {
          displayValue = Math.round(p.value); // Força inteiro
        } else {
          displayValue = typeof p.value === 'number' ? p.value.toFixed(unit === 'ms' ? 0 : 2) : p.value;
          displayValue += (unit ?? ''); // Adiciona 's' ou 'ms'
        }

        return (
          <p key={p.dataKey} style={{ color: p.color, margin: 0 }}>
            {p.name}: <span style={{ fontWeight: 700 }}>{displayValue}</span>
          </p>
        );
      })}
    </div>
  );
}

function CustomDot({ cx, cy }) {
  if (!cx || !cy) return null;
  return (
    <g>
      <circle cx={cx} cy={cy} r={3}   fill="#3b82f6" />
      <circle cx={cx} cy={cy} r={5.5} fill="none" stroke="#3b82f6" strokeWidth={1} strokeOpacity={0.3} />
    </g>
  );
}

function CustomActiveDot({ cx, cy }) {
  if (!cx || !cy) return null;
  return (
    <g>
      <circle cx={cx} cy={cy} r={4} fill="#3b82f6" />
      <circle cx={cx} cy={cy} r={8} fill="none" stroke="#3b82f6" strokeWidth={1.5} strokeOpacity={0.2} />
    </g>
  );
}

export default function ProjectDetails() {
  const { id } = useParams();
  const navigate = useNavigate();

  const [isLoading, setIsLoading]       = useState(true);
  const [isRefreshing, setIsRefreshing] = useState(false);
  const [timeUnit, setTimeUnit]         = useState('s');
  const [chartMode, setChartMode]       = useState('duration');

  const [filters, setFilters]                 = useState({ suites: [], versions: [] });
  const [selectedSuite, setSelectedSuite]     = useState('');
  const [selectedVersion, setSelectedVersion] = useState('');

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

  useEffect(() => {
    if (!id) return;
    api.get(`api/v1/projects/${id}/dashboard/filters`)
      .then(res => setFilters({ suites: res.data.suites || [], versions: res.data.versions || [] }))
      .catch(err => console.error('Erro ao carregar filtros:', err));
  }, [id]);

  const fetchDashboardData = useCallback(async (showRefreshSpinner = false) => {
    try {
      if (showRefreshSpinner) setIsRefreshing(true);
      else setIsLoading(true);

      const params = new URLSearchParams();
      if (selectedSuite)   params.append('suiteName',   selectedSuite);
      if (selectedVersion) params.append('versionName', selectedVersion);

      const qs      = params.toString() ? `?${params.toString()}` : '';
      const qsExtra = params.toString() ? `&${params.toString()}` : '';

      const [metricsRes, historyRes] = await Promise.all([
        api.get(`api/v1/projects/${id}/dashboard/metrics${qs}`),
        api.get(`api/v1/projects/${id}/dashboard/history?size=100${qsExtra}`),
      ]);

      setMetrics(metricsRes.data);

      const content = historyRes.data.content || [];
      let dataToDisplay = content;

      if (!selectedVersion) {
        const seen = new Set();
        dataToDisplay = content.filter(exec => {
          if (exec.versionName === 'N/A') return true;
          if (seen.has(exec.versionName)) return false;
          seen.add(exec.versionName);
          return true;
        });
      }

      setHistoryData(
        dataToDisplay.map((exec, idx) => {
          const timeString = new Date(exec.startTime).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
          return {
            name: !selectedVersion
              ? (exec.versionName !== 'N/A' ? exec.versionName : `Run ${idx}`)
              : (exec.versionName !== 'N/A' ? `${exec.versionName} (${timeString})` : `Run ${timeString}`),
            rawDuration: exec.durationMillis || 0,
            passedCount: exec.passedCount    || 0,
            failedCount: exec.failedCount    || 0,
            id:          exec.executionId,
          };
        }).reverse()
      );
    } catch {
      toast.error('Erro ao sincronizar dados com o servidor.');
    } finally {
      setIsLoading(false);
      setIsRefreshing(false);
    }
  }, [id, selectedSuite, selectedVersion]);

  useEffect(() => {
    if (id) fetchDashboardData();
  }, [fetchDashboardData, id]);

  const chartData = historyData.map(item => {
    let value = item.rawDuration;
    if (timeUnit === 's') value /= 1000;
    if (timeUnit === 'm') value /= 60000;
    return { ...item, duration: Number(value.toFixed(timeUnit === 'ms' ? 0 : 2)) };
  });

  const handleChartClick = useCallback((state) => {
    if (!state?.activeLabel) return;
    const hit = chartData.find(item => item.name === state.activeLabel);
    if (hit?.id) navigate(`/projects/${id}/executions/${hit.id}`);
  }, [chartData, id, navigate]);

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
              title="Atualizar dados"
              className="p-1.5 rounded-lg text-slate-400 hover:text-blue-500 hover:bg-blue-50 transition-colors disabled:opacity-40"
            >
              <RefreshCw size={16} className={isRefreshing ? 'animate-spin' : ''} />
            </button>
          </div>
          {metrics.lastExecutionTime && (
            <p className="text-sm text-slate-400 flex items-center gap-1.5 mt-1">
              <Clock size={13} /> Atualizado: {metrics.lastExecutionTime}
            </p>
          )}
        </div>

        {/* Filtros */}
        <div className="flex flex-col sm:flex-row items-center gap-3 bg-white p-3 rounded-xl border border-slate-200 shadow-sm">
          <div className="flex items-center gap-2 text-slate-400">
            <Filter size={15} />
            <span className="text-xs font-semibold uppercase tracking-wider">Filtros</span>
          </div>
          <select value={selectedSuite} onChange={e => setSelectedSuite(e.target.value)}
            className="bg-slate-50 border border-slate-200 text-slate-700 text-sm rounded-lg px-3 py-1.5 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-blue-500 cursor-pointer">
            <option value="">Todas as Suites</option>
            {filters.suites.map(s => <option key={s} value={s}>{s}</option>)}
          </select>
          <select value={selectedVersion} onChange={e => setSelectedVersion(e.target.value)}
            className="bg-slate-50 border border-slate-200 text-slate-700 text-sm rounded-lg px-3 py-1.5 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-blue-500 cursor-pointer">
            <option value="">Todas as Versões</option>
            {filters.versions.map(v => <option key={v} value={v}>{v}</option>)}
          </select>
          {(selectedSuite || selectedVersion) && (
            <button onClick={() => { setSelectedSuite(''); setSelectedVersion(''); }}
              className="text-xs text-blue-600 hover:underline whitespace-nowrap">
              Limpar filtros
            </button>
          )}
        </div>
      </div>

      {/* ── Skeleton ── */}
      {isLoading ? (
        <div className="space-y-6">
          <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
            <SkeletonCard /><SkeletonCard /><SkeletonCard />
          </div>
          <div className="bg-white rounded-xl border border-slate-200 p-6 h-64 animate-pulse">
            <div className="h-full bg-slate-100 rounded-lg" />
          </div>
        </div>
      ) : (
        <>
          {/* ── Cards de métricas ── */}
          <div className="grid grid-cols-1 md:grid-cols-3 gap-6 mb-8">
            <MetricCard
              label="Sucesso da Suite" value={`${metrics.healthScore}%`} accent="green"
              sub={metrics.healthScore >= 90 ? 'Estável' : metrics.healthScore >= 70 ? 'Atenção recomendada' : 'Requer ação imediata'}
            />
            <MetricCard label="Total de Builds" value={metrics.totalExecutions} accent="blue" />
            <MetricCard
              label="Testes Instáveis" value={metrics.totalFlaky} accent="red"
              sub={metrics.totalFlaky === 0 ? 'Nenhuma instabilidade detetada' : `${metrics.totalFlaky} teste(s) com comportamento errático`}
            />
          </div>

          {/* ── Gráfico ── */}
          <div className="bg-white rounded-xl border border-slate-200 p-5 mb-8 shadow-sm">

            {/* Header */}
            <div className="flex flex-col sm:flex-row items-start sm:items-center justify-between mb-4 gap-3">
              <div className="flex items-center gap-2">
                <TrendingUp size={15} className="text-blue-500" />
                <h3 className="font-semibold text-slate-700 text-xs uppercase tracking-wider">Histórico de execuções</h3>
                <span className="hidden sm:inline text-[10px] text-slate-300 ml-1">· clique para detalhes</span>
              </div>

              <div className="flex items-center gap-2">
                {/* Toggle modo */}
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

                {/* Toggle unidade */}
                {chartMode === 'duration' && (
                  <div className="flex bg-slate-50 border border-slate-200 rounded-lg p-0.5 gap-0.5">
                    {[{ key: 'ms', label: 'ms' }, { key: 's', label: 's' }, { key: 'm', label: 'min' }].map(({ key, label }) => (
                      <button key={key} onClick={() => setTimeUnit(key)}
                        className={`px-2.5 py-1 text-[11px] font-medium rounded-md transition-all ${
                          timeUnit === key ? 'bg-white text-blue-600 shadow-sm' : 'text-slate-400 hover:text-slate-600'
                        }`}>
                        {label}
                      </button>
                    ))}
                  </div>
                )}
              </div>
            </div>

            <div className="h-52">
              {chartData.length > 0 ? (
                <ResponsiveContainer width="100%" height="100%">
                  {chartMode === 'duration' ? (
                    // CORREÇÃO 1: Remover o left: -16 e dar um left: 0 (ou maior) para o YAxis caber
                    <AreaChart data={chartData} onClick={handleChartClick} style={{ cursor: 'pointer' }}
                      margin={{ top: 8, right: 8, left: 0, bottom: 0 }}>
                      <defs>
                        <linearGradient id="grad" x1="0" y1="0" x2="0" y2="1">
                          <stop offset="0%"   stopColor="#3b82f6" stopOpacity={0.08} />
                          <stop offset="100%" stopColor="#3b82f6" stopOpacity={0} />
                        </linearGradient>
                      </defs>
                      <CartesianGrid horizontal={true} vertical={false} stroke="#f1f5f9" />
                      <XAxis dataKey="name" tick={{ fill: '#cbd5e1', fontSize: 10 }} axisLine={false} tickLine={false} dy={6} interval="preserveStartEnd" />
                      {/* Adicionado width={45} no YAxis para garantir espaço suficiente para os números */}
                      <YAxis tick={{ fill: '#cbd5e1', fontSize: 10 }} axisLine={false} tickLine={false} unit={timeUnit} width={45} tickCount={4} />
                      <Tooltip content={<CustomTooltip unit={timeUnit} mode={chartMode} />} />
                      <Area type="monotone" dataKey="duration" name={`Duração (${timeUnit})`}
                        stroke="#3b82f6" strokeWidth={1.5} fill="url(#grad)"
                        dot={<CustomDot />} activeDot={<CustomActiveDot />} />
                    </AreaChart>
                  ) : (
                    // CORREÇÃO 1: O mesmo no BarChart
                    <BarChart data={chartData} onClick={handleChartClick} style={{ cursor: 'pointer' }}
                      margin={{ top: 8, right: 8, left: 0, bottom: 0 }}
                      barCategoryGap="40%" barGap={1}>
                      <CartesianGrid horizontal={true} vertical={false} stroke="#f1f5f9" />
                      <XAxis dataKey="name" tick={{ fill: '#cbd5e1', fontSize: 10 }} axisLine={false} tickLine={false} dy={6} interval="preserveStartEnd" />
                      <YAxis tick={{ fill: '#cbd5e1', fontSize: 10 }} axisLine={false} tickLine={false} width={36} tickCount={4} />
                      <Tooltip content={<CustomTooltip mode={chartMode} />} />
                      <Bar dataKey="passedCount" name="Passou" stackId="a" fill="#86efac" radius={[0, 0, 2, 2]} />
                      <Bar dataKey="failedCount" name="Falhou" stackId="a" fill="#fca5a5" radius={[2, 2, 0, 0]} />
                    </BarChart>
                  )}
                </ResponsiveContainer>
              ) : (
                <div className="flex flex-col items-center justify-center h-full gap-2">
                  <BarChart2 size={24} className="text-slate-200" />
                  <p className="text-xs text-slate-300 italic">
                    {selectedSuite || selectedVersion ? 'Sem dados para os filtros selecionados.' : 'Sem execuções registadas.'}
                  </p>
                </div>
              )}
            </div>

            {/* Legenda — só no modo resultados */}
            {chartMode === 'results' && chartData.length > 0 && (
              <div className="flex items-center gap-4 mt-3 justify-end">
                <span className="flex items-center gap-1.5 text-[10px] text-slate-400">
                  <span className="w-2.5 h-2.5 rounded-sm bg-green-300 inline-block" /> Passou
                </span>
                <span className="flex items-center gap-1.5 text-[10px] text-slate-400">
                  <span className="w-2.5 h-2.5 rounded-sm bg-red-300 inline-block" /> Falhou
                </span>
              </div>
            )}
          </div>

          {/* ── Tabelas ── */}
          <div className="grid grid-cols-1 lg:grid-cols-2 gap-8">

            {/* Falhas críticas */}
            <div className="bg-white rounded-xl border border-slate-200 shadow-sm overflow-hidden flex flex-col">
              <div className="px-6 py-4 border-b border-slate-100 flex items-center justify-between">
                <div className="flex items-center gap-2">
                  <XCircle size={16} className="text-red-500" />
                  <h3 className="font-bold text-slate-800 text-xs uppercase tracking-wider">Falhas Críticas</h3>
                </div>
                {metrics.recentFailures?.length > 0 && (
                  <span className="text-[10px] font-bold px-2 py-0.5 rounded-full bg-red-100 text-red-600">
                    {metrics.recentFailures.length}
                  </span>
                )}
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
                    <p className="text-sm italic">Nenhuma falha na última execução.</p>
                  </div>
                )}
              </div>
            </div>

            {/* Ranking Flaky */}
            <div className="bg-white rounded-xl border border-slate-200 shadow-sm overflow-hidden flex flex-col">
              <div className="px-6 py-4 border-b border-slate-100 flex items-center justify-between">
                <div className="flex items-center gap-2">
                  <AlertTriangle size={16} className="text-amber-500" />
                  <h3 className="font-bold text-slate-800 text-xs uppercase tracking-wider">Ranking de Instabilidade</h3>
                </div>
                {metrics.flakyTests?.length > 0 && (
                  <span className="text-[10px] font-bold px-2 py-0.5 rounded-full bg-amber-100 text-amber-600">
                    {metrics.flakyTests.length}
                  </span>
                )}
              </div>
              <div className="overflow-x-auto flex-1">
                {metrics.flakyTests?.length > 0 ? (
                  <table className="min-w-full divide-y divide-slate-100">
                    <tbody className="divide-y divide-slate-100 text-sm">
                      {metrics.flakyTests.map((test, i) => {
                        const rateNum     = typeof test.failureRate === 'string' ? parseFloat(test.failureRate) : (test.failureRate || 0) * 100;
                        const rateDisplay = isNaN(rateNum) ? test.failureRate : `${Math.round(rateNum)}%`;
                        const barWidth    = isNaN(rateNum) ? 0 : Math.min(rateNum, 100);
                        const barColor    = rateNum >= 60 ? 'bg-red-400' : rateNum >= 30 ? 'bg-amber-400' : 'bg-yellow-300';
                        return (
                          <tr key={test.id} className="hover:bg-amber-50/40 transition-colors">
                            <td className="px-6 py-3">
                              <div className="flex items-start gap-2">
                                <span className="text-[10px] font-bold text-slate-400 mt-0.5 shrink-0">#{i + 1}</span>
                                <span className="text-xs font-mono text-slate-600 break-all">{test.name}</span>
                              </div>
                            </td>
                            <td className="px-6 py-3 text-right w-28 shrink-0">
                              <div className="flex flex-col items-end gap-1">
                                <span className="text-[10px] font-bold text-amber-600 uppercase tracking-wide">{rateDisplay}</span>
                                <div className="w-16 h-1.5 bg-slate-100 rounded-full overflow-hidden">
                                  <div className={`h-full rounded-full transition-all ${barColor}`} style={{ width: `${barWidth}%` }} />
                                </div>
                              </div>
                            </td>
                          </tr>
                        );
                      })}
                    </tbody>
                  </table>
                ) : (
                  <div className="flex flex-col items-center justify-center py-12 gap-2 text-slate-400">
                    <span className="text-2xl">🟢</span>
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