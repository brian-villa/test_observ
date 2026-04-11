import { useState, useEffect } from 'react';
import { useParams, Link, useNavigate } from 'react-router-dom';
import Layout from '../components/Layout';
import { 
  ArrowLeft, Activity, AlertTriangle, XCircle, Clock, TrendingUp, Filter 
} from 'lucide-react';
import api from '../services/api';
import toast from 'react-hot-toast';
import { 
  XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer, AreaChart, Area 
} from 'recharts';

export default function ProjectDetails() {
  const { id } = useParams();
  const navigate = useNavigate();
  const [isLoading, setIsLoading] = useState(true);
  const [timeUnit, setTimeUnit] = useState('s');

  const [filters, setFilters] = useState({ suites: [], versions: [] });
  const [selectedSuite, setSelectedSuite] = useState('');
  const [selectedVersion, setSelectedVersion] = useState('');

  const [metrics, setMetrics] = useState({
    projectName: "A carregar...",
    healthScore: 0,
    totalExecutions: 0,
    totalFlaky: 0,
    lastExecutionTime: "",
    recentFailures: [],
    flakyTests: []
  });

  const [historyData, setHistoryData] = useState([]);

  useEffect(() => {
    const fetchFilters = async () => {
      try {
        const res = await api.get(`api/v1/projects/${id}/dashboard/filters`);
        setFilters({ suites: res.data.suites || [], versions: res.data.versions || [] });
      } catch (err) {
        console.error("Erro ao carregar filtros:", err);
      }
    };
    if (id) fetchFilters();
  }, [id]);

  useEffect(() => {
    const fetchDashboardData = async () => {
      try {
        setIsLoading(true);
        const params = new URLSearchParams();
        if (selectedSuite) params.append('suiteName', selectedSuite);
        if (selectedVersion) params.append('versionName', selectedVersion);
        
        const queryString = params.toString() ? `?${params.toString()}` : '';
        const historyQueryString = params.toString() ? `&${params.toString()}` : '';

        const metricsRes = await api.get(`api/v1/projects/${id}/dashboard/metrics${queryString}`);
        setMetrics(metricsRes.data);

        const historyRes = await api.get(`api/v1/projects/${id}/dashboard/history?size=100${historyQueryString}`);
        const content = historyRes.data.content || [];

        let dataToDisplay = content;

        if (!selectedVersion) {
          const latestAttemptsOnly = [];
          const seenVersions = new Set();
          for (const exec of content) {
            if (exec.versionName !== 'N/A') {
              if (seenVersions.has(exec.versionName)) continue;
              seenVersions.add(exec.versionName);
            }
            latestAttemptsOnly.push(exec);
          }
          dataToDisplay = latestAttemptsOnly;
        }
        
        const formattedHistory = dataToDisplay.map((exec, idx) => {
          const timeString = new Date(exec.startTime).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
          return {
            name: !selectedVersion 
              ? (exec.versionName !== 'N/A' ? exec.versionName : `Run ${idx}`)
              : (exec.versionName !== 'N/A' ? `${exec.versionName} (${timeString})` : `Run ${timeString}`),
            rawDuration: exec.durationMillis || 0,
            id: exec.executionId
          };
        }).reverse();

        setHistoryData(formattedHistory);
      } catch (error) {
        toast.error('Erro ao sincronizar dados com o servidor.');
      } finally {
        setIsLoading(false);
      }
    };

    if (id) fetchDashboardData();
  }, [id, selectedSuite, selectedVersion]);

  // FUNÇÃO ATUALIZADA: Apenas navega para a nova página
  const handleChartClick = (state) => {
    if (!state || !state.activeLabel) return;
    const clickedData = chartData.find(item => item.name === state.activeLabel);

    if (clickedData && clickedData.id) {
      // Redireciona para a nova rota, passando o ID do projeto e da execução
      navigate(`/projects/${id}/executions/${clickedData.id}`);
    } else {
      console.warn("Execução não tem ID associado.");
    }
  };

  const chartData = historyData.map(item => {
    let value = item.rawDuration;
    if (timeUnit === 's') value = value / 1000;
    if (timeUnit === 'm') value = value / 60000;
    return { ...item, duration: Number(value.toFixed(timeUnit === 'ms' ? 0 : 2)) };
  });

  return (
    <Layout>
      <div className="mb-6 flex flex-col md:flex-row md:items-center justify-between gap-4">
        <div>
          <Link to="/dashboard" className="inline-flex items-center text-sm font-medium text-slate-500 mb-4 hover:text-slate-700 transition-colors">
            <ArrowLeft size={16} className="mr-1" /> Voltar ao Gestor
          </Link>
          <h1 className="text-3xl font-bold text-slate-900">{metrics.projectName}</h1>
          <p className="text-sm text-slate-500 flex items-center gap-2 mt-1 italic">
            <Clock size={14} /> Atualizado: {metrics.lastExecutionTime}
          </p>
        </div>

        <div className="flex flex-col sm:flex-row items-center gap-3 bg-white p-3 rounded-lg border border-slate-200 shadow-sm">
          <div className="flex items-center gap-2 text-slate-500">
            <Filter size={16} />
            <span className="text-sm font-semibold uppercase tracking-wider">Filtros:</span>
          </div>
          <select value={selectedSuite} onChange={(e) => setSelectedSuite(e.target.value)} className="form-select bg-slate-50 border border-slate-200 text-slate-700 text-sm rounded-md px-3 py-1.5 focus:ring-blue-500 outline-none">
            <option value="">Todas as Suites</option>
            {filters.suites.map(suite => <option key={suite} value={suite}>{suite}</option>)}
          </select>
          <select value={selectedVersion} onChange={(e) => setSelectedVersion(e.target.value)} className="form-select bg-slate-50 border border-slate-200 text-slate-700 text-sm rounded-md px-3 py-1.5 focus:ring-blue-500 outline-none">
            <option value="">Todas as Versões</option>
            {filters.versions.map(version => <option key={version} value={version}>{version}</option>)}
          </select>
        </div>
      </div>

      {isLoading ? (
        <div className="flex flex-col justify-center items-center h-400px gap-4">
          <Activity className="text-blue-500 animate-spin" size={40} />
          <p className="text-slate-500 font-medium">Sincronizando análise histórica...</p>
        </div>
      ) : (
        <>
          <div className="grid grid-cols-1 md:grid-cols-3 gap-6 mb-8">
            <div className="bg-white p-6 rounded-xl border border-slate-200 shadow-sm border-t-4 border-t-green-500">
              <p className="text-xs font-bold text-slate-500 uppercase tracking-widest">Sucesso da Suite</p>
              <div className="flex items-end gap-2 mt-3"><span className="text-4xl font-black text-slate-900">{metrics.healthScore}%</span></div>
            </div>
            <div className="bg-white p-6 rounded-xl border border-slate-200 shadow-sm border-t-4 border-t-blue-500">
              <p className="text-xs font-bold text-slate-500 uppercase tracking-widest">Total de Builds</p>
              <div className="mt-3"><span className="text-4xl font-black text-slate-900">{metrics.totalExecutions}</span></div>
            </div>
            <div className="bg-white p-6 rounded-xl border border-red-100 shadow-sm border-t-4 border-t-red-500">
              <p className="text-xs font-bold text-red-600 uppercase tracking-widest">Testes Instáveis</p>
              <div className="mt-3"><span className="text-4xl font-black text-red-700">{metrics.totalFlaky}</span></div>
            </div>
          </div>

          <div className="bg-white rounded-xl border border-slate-200 p-6 mb-8 shadow-sm">
            <div className="flex flex-col sm:flex-row items-start sm:items-center justify-between mb-8 gap-4">
              <div className="flex items-center gap-2">
                <TrendingUp size={20} className="text-blue-500" />
                <h3 className="font-bold text-slate-800 uppercase text-sm tracking-wider">Histórico de Tempo de Execução</h3>
                <span className="text-xs text-slate-400 font-normal ml-2">(Clique num ponto para detalhes)</span>
              </div>
              <div className="flex bg-slate-100 p-1 rounded-lg border border-slate-200">
                <button onClick={() => setTimeUnit('ms')} className={`px-3 py-1 text-xs font-semibold rounded-md ${timeUnit === 'ms' ? 'bg-white shadow-sm text-blue-600' : 'text-slate-500'}`}>ms</button>
                <button onClick={() => setTimeUnit('s')} className={`px-3 py-1 text-xs font-semibold rounded-md ${timeUnit === 's' ? 'bg-white shadow-sm text-blue-600' : 'text-slate-500'}`}>Segundos</button>
                <button onClick={() => setTimeUnit('m')} className={`px-3 py-1 text-xs font-semibold rounded-md ${timeUnit === 'm' ? 'bg-white shadow-sm text-blue-600' : 'text-slate-500'}`}>Minutos</button>
              </div>
            </div>
            
            <div className="h-80 w-full min-h-320px">
              {chartData.length > 0 ? (
                <ResponsiveContainer width="100%" height="100%">
                  <AreaChart data={chartData} onClick={handleChartClick} style={{ cursor: 'pointer' }}>
                    <defs>
                      <linearGradient id="colorDuration" x1="0" y1="0" x2="0" y2="1">
                        <stop offset="5%" stopColor="#3b82f6" stopOpacity={0.2}/>
                        <stop offset="95%" stopColor="#3b82f6" stopOpacity={0}/>
                      </linearGradient>
                    </defs>
                    <CartesianGrid strokeDasharray="3 3" vertical={false} stroke="#f1f5f9" />
                    <XAxis dataKey="name" tick={{fill: '#94a3b8', fontSize: 12}} dy={10} axisLine={false} tickLine={false} />
                    <YAxis tick={{fill: '#94a3b8', fontSize: 12}} axisLine={false} tickLine={false} unit={timeUnit} />
                    <Tooltip contentStyle={{ borderRadius: '12px', border: 'none', boxShadow: '0 10px 15px -3px rgb(0 0 0 / 0.1)' }} />
                    <Area type="monotone" dataKey="duration" name={`Duração (${timeUnit})`} stroke="#3b82f6" fill="url(#colorDuration)" strokeWidth={3} activeDot={{ r: 6 }} />
                  </AreaChart>
                </ResponsiveContainer>
              ) : (
                <div className="flex items-center justify-center h-full text-slate-400 italic">Nenhum dado encontrado.</div>
              )}
            </div>
          </div>

          <div className="grid grid-cols-1 lg:grid-cols-2 gap-8">
            <div className="bg-white rounded-xl border border-slate-200 shadow-sm overflow-hidden flex flex-col">
              <div className="px-6 py-4 border-b border-slate-200 flex items-center gap-2">
                <XCircle size={18} className="text-red-500" />
                <h3 className="font-bold text-slate-800 text-sm uppercase tracking-wider">Falhas Críticas (Última Run)</h3>
              </div>
              <div className="overflow-x-auto">
                <table className="min-w-full divide-y divide-slate-100">
                  <tbody className="divide-y divide-slate-100 text-sm">
                    {metrics.recentFailures?.map((test) => (
                      <tr key={test.id} className="hover:bg-red-50/30 transition-colors">
                        <td className="px-6 py-4 text-xs font-mono text-slate-600 break-all">{test.name}</td>
                        <td className="px-6 py-4 text-right"><span className="text-[10px] font-bold uppercase px-2 py-1 bg-red-100 text-red-600 rounded">{test.status}</span></td>
                      </tr>
                    ))}
                    {(!metrics.recentFailures || metrics.recentFailures.length === 0) && (
                      <tr><td colSpan="2" className="p-12 text-center text-slate-400 italic text-sm">Nenhuma falha reportada.</td></tr>
                    )}
                  </tbody>
                </table>
              </div>
            </div>

            <div className="bg-white rounded-xl border border-slate-200 shadow-sm overflow-hidden flex flex-col">
              <div className="px-6 py-4 border-b border-slate-200 flex items-center gap-2">
                <AlertTriangle size={18} className="text-amber-500" />
                <h3 className="font-bold text-slate-800 text-sm uppercase tracking-wider">Ranking de Instabilidade (Flaky)</h3>
              </div>
              <div className="overflow-x-auto">
                <table className="min-w-full divide-y divide-slate-100">
                  <tbody className="divide-y divide-slate-100 text-sm">
                    {metrics.flakyTests?.map((test) => (
                      <tr key={test.id} className="hover:bg-amber-50/30 transition-colors">
                        <td className="px-6 py-4 text-xs font-mono text-slate-600 break-all">{test.name}</td>
                        <td className="px-6 py-4 text-right"><span className="font-bold text-amber-600 text-[10px] uppercase tracking-widest">Risco {test.failureRate}</span></td>
                      </tr>
                    ))}
                    {(!metrics.flakyTests || metrics.flakyTests.length === 0) && (
                      <tr><td colSpan="2" className="p-12 text-center text-slate-400 italic text-sm">Sem testes instáveis detetados.</td></tr>
                    )}
                  </tbody>
                </table>
              </div>
            </div>
          </div>
        </>
      )}
    </Layout>
  );
}