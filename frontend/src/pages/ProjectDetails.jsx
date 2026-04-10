import { useState, useEffect } from 'react';
import { useParams, Link } from 'react-router-dom';
import Layout from '../components/Layout';
import { 
  ArrowLeft, Activity, AlertTriangle, CheckCircle2, 
  XCircle, Clock, ShieldAlert, BarChart3, TrendingUp 
} from 'lucide-react';
import api from '../services/api';
import toast from 'react-hot-toast';
import { 
  LineChart, Line, XAxis, YAxis, CartesianGrid, 
  Tooltip, ResponsiveContainer, AreaChart, Area 
} from 'recharts';

/**
 * Página de Detalhes e Análise do Projeto.
 * Consome métricas agregadas e histórico de execuções do Backend.
 */
export default function ProjectDetails() {
  const { id } = useParams();
  const [isLoading, setIsLoading] = useState(true);

  const [timeUnit, setTimeUnit] = useState('s');

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
    const fetchDashboardData = async () => {
      try {
        setIsLoading(true);
        const metricsRes = await api.get(`api/v1/projects/${id}/dashboard/metrics`);
        setMetrics(metricsRes.data);

        const historyRes = await api.get(`api/v1/projects/${id}/dashboard/history?size=10`);
        const content = historyRes.data.content || [];
        
        const formattedHistory = content.map((exec, idx) => ({
          name: exec.versionName !== 'N/A' ? exec.versionName : `Run ${content.length - idx}`,
          rawDuration: exec.durationMillis || 0,
        })).reverse();

        setHistoryData(formattedHistory);
      } catch (error) {
        console.error("Erro na análise de dados:", error);
        toast.error('Erro ao sincronizar dados com o servidor.');
      } finally {
        setIsLoading(false);
      }
    };

    if (id) fetchDashboardData();
  }, [id]);

  const chartData = historyData.map(item => {
    let value = item.rawDuration;
    if (timeUnit === 's') value = value / 1000;
    if (timeUnit === 'm') value = value / 60000;
    
    return {
      ...item,
      duration: Number(value.toFixed(timeUnit === 'ms' ? 0 : 2))
    };
  });

  if (isLoading) {
    return (
      <Layout>
        <div className="flex flex-col justify-center items-center h-[400px] gap-4">
          <Activity className="text-blue-500 animate-spin" size={40} />
          <p className="text-slate-500 font-medium">Sincronizando análise histórica...</p>
        </div>
      </Layout>
    );
  }

  return (
    <Layout>
      <div className="mb-8">
        <Link to="/dashboard" className="inline-flex items-center text-sm font-medium text-slate-500 mb-4 hover:text-slate-700 transition-colors">
          <ArrowLeft size={16} className="mr-1" /> Voltar ao Gestor
        </Link>
        <h1 className="text-3xl font-bold text-slate-900">{metrics.projectName}</h1>
        <p className="text-sm text-slate-500 flex items-center gap-2 mt-1 italic">
          <Clock size={14} /> Atualizado: {metrics.lastExecutionTime}
        </p>
      </div>

      {/* Cartões de KPI */}
      <div className="grid grid-cols-1 md:grid-cols-3 gap-6 mb-8">
        <div className="bg-white p-6 rounded-xl border border-slate-200 shadow-sm border-t-4 border-t-green-500">
          <p className="text-xs font-bold text-slate-500 uppercase tracking-widest">Sucesso da Suite</p>
          <div className="flex items-end gap-2 mt-3">
            <span className="text-4xl font-black text-slate-900">{metrics.healthScore}%</span>
          </div>
        </div>

        <div className="bg-white p-6 rounded-xl border border-slate-200 shadow-sm border-t-4 border-t-blue-500">
          <p className="text-xs font-bold text-slate-500 uppercase tracking-widest">Total de Builds</p>
          <div className="mt-3">
            <span className="text-4xl font-black text-slate-900">{metrics.totalExecutions}</span>
          </div>
        </div>

        <div className="bg-white p-6 rounded-xl border border-red-100 shadow-sm border-t-4 border-t-red-500">
          <p className="text-xs font-bold text-red-600 uppercase tracking-widest">Testes Instáveis</p>
          <div className="mt-3">
            <span className="text-4xl font-black text-red-700">{metrics.totalFlaky}</span>
          </div>
        </div>
      </div>

      {/* GRÁFICO DE ANÁLISE HISTORIAL */}
      <div className="bg-white rounded-xl border border-slate-200 p-6 mb-8 shadow-sm">
        <div className="flex flex-col sm:flex-row items-start sm:items-center justify-between mb-8 gap-4">
          <div className="flex items-center gap-2">
            <TrendingUp size={20} className="text-blue-500" />
            <h3 className="font-bold text-slate-800 uppercase text-sm tracking-wider">Histórico de Tempo de Execução</h3>
          </div>
          
          {/* INTERFACE UX: Seletor de Unidade de Tempo */}
          <div className="flex bg-slate-100 p-1 rounded-lg border border-slate-200">
            <button 
              onClick={() => setTimeUnit('ms')}
              className={`px-3 py-1 text-xs font-semibold rounded-md transition-all ${timeUnit === 'ms' ? 'bg-white shadow-sm text-blue-600' : 'text-slate-500 hover:text-slate-700'}`}
            >
              ms
            </button>
            <button 
              onClick={() => setTimeUnit('s')}
              className={`px-3 py-1 text-xs font-semibold rounded-md transition-all ${timeUnit === 's' ? 'bg-white shadow-sm text-blue-600' : 'text-slate-500 hover:text-slate-700'}`}
            >
              Segundos
            </button>
            <button 
              onClick={() => setTimeUnit('m')}
              className={`px-3 py-1 text-xs font-semibold rounded-md transition-all ${timeUnit === 'm' ? 'bg-white shadow-sm text-blue-600' : 'text-slate-500 hover:text-slate-700'}`}
            >
              Minutos
            </button>
          </div>
        </div>
        
        <div className="h-80 w-full min-h-[320px]">
          {chartData.length > 0 ? (
            <ResponsiveContainer width="100%" height="100%">
              <AreaChart data={chartData}>
                <defs>
                  <linearGradient id="colorDuration" x1="0" y1="0" x2="0" y2="1">
                    <stop offset="5%" stopColor="#3b82f6" stopOpacity={0.2}/>
                    <stop offset="95%" stopColor="#3b82f6" stopOpacity={0}/>
                  </linearGradient>
                </defs>
                <CartesianGrid strokeDasharray="3 3" vertical={false} stroke="#f1f5f9" />
                <XAxis dataKey="name" tick={{fill: '#94a3b8', fontSize: 12}} dy={10} axisLine={false} tickLine={false} />
                
                {/* A Unidade do Eixo Y muda dinamicamente */}
                <YAxis tick={{fill: '#94a3b8', fontSize: 12}} axisLine={false} tickLine={false} unit={timeUnit} />
                
                <Tooltip contentStyle={{ borderRadius: '12px', border: 'none', boxShadow: '0 10px 15px -3px rgb(0 0 0 / 0.1)' }} />
                
                <Area 
                  type="monotone" 
                  dataKey="duration" 
                  name={`Duração (${timeUnit})`} 
                  stroke="#3b82f6" 
                  fill="#eff6ff" 
                  strokeWidth={3} 
                  activeDot={{ r: 6 }}
                />
              </AreaChart>
            </ResponsiveContainer>
          ) : (
            <div className="flex items-center justify-center h-full text-slate-400 italic">
              Aguardando dados históricos para gerar o gráfico...
            </div>
          )}
        </div>
      </div>

      {/* Tabelas Omitidas (mantém o teu código existente para as tabelas) */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-8">
        {/* Lista de Falhas Recentes vindas do Backend */}
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
                {metrics.recentFailures?.length === 0 && (
                  <tr><td colSpan="2" className="p-12 text-center text-slate-400 italic text-sm">Nenhuma falha reportada.</td></tr>
                )}
              </tbody>
            </table>
          </div>
        </div>

        {/* Lista de Testes Flaky detetados pelo algoritmo de histórico */}
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
                {metrics.flakyTests?.length === 0 && (
                  <tr><td colSpan="2" className="p-12 text-center text-slate-400 italic text-sm">Sem testes instáveis detetados.</td></tr>
                )}
              </tbody>
            </table>
          </div>
        </div>
      </div>
    </Layout>
  );
}