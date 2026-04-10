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
  const [metrics, setMetrics] = useState(null);
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
          duration: exec.durationMinutes,
          status: exec.hasFailures ? 0 : 1,
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

  if (isLoading || !metrics) {
    return (
      <Layout>
        <div className="flex flex-col justify-center items-center h-96 gap-4">
          <Activity className="text-primary-500 animate-spin" size={40} />
          <p className="text-slate-500 font-medium animate-pulse">Gerando relatórios analíticos...</p>
        </div>
      </Layout>
    );
  }

  return (
    <Layout>
      {/* Cabeçalho */}
      <div className="mb-8">
        <Link to="/dashboard" className="inline-flex items-center text-sm font-medium text-slate-500 hover:text-slate-700 mb-4 transition-colors">
          <ArrowLeft size={16} className="mr-1" /> Voltar ao Gestor
        </Link>
        <div className="flex items-center justify-between">
          <div>
            <h1 className="text-3xl font-bold text-slate-900 tracking-tight">
              {metrics.projectName}
            </h1>
            <p className="text-sm text-slate-500 mt-2 flex items-center gap-2">
              <Clock size={14} /> Última sincronização: <span className="font-semibold text-slate-700">{metrics.lastExecutionTime}</span>
            </p>
          </div>
          <div className="flex gap-3">
            <button className="bg-white px-4 py-2 border border-slate-300 rounded-lg text-sm font-medium text-slate-700 hover:bg-slate-50 shadow-sm transition-all">
              Configurações
            </button>
          </div>
        </div>
      </div>

      {/* Cartões de KPI - Dados Calculados no Backend */}
      <div className="grid grid-cols-1 md:grid-cols-3 gap-6 mb-8">
        <div className="bg-white rounded-xl border border-slate-200 p-6 shadow-sm">
          <div className="flex justify-between items-start">
            <p className="text-sm font-semibold text-slate-500 uppercase">Saúde Atual</p>
            <Activity size={20} className={metrics.healthScore > 80 ? "text-green-500" : "text-amber-500"} />
          </div>
          <div className="mt-4 flex items-baseline gap-2">
            <span className="text-4xl font-extrabold text-slate-900">{metrics.healthScore}%</span>
            <span className="text-sm text-slate-400">sucesso</span>
          </div>
          <div className="mt-4 w-full bg-slate-100 rounded-full h-2">
            <div 
              className={`h-2 rounded-full transition-all duration-1000 ${metrics.healthScore > 80 ? 'bg-green-500' : 'bg-amber-500'}`} 
              style={{ width: `${metrics.healthScore}%` }}
            ></div>
          </div>
        </div>

        <div className="bg-white rounded-xl border border-slate-200 p-6 shadow-sm">
          <div className="flex justify-between items-start">
            <p className="text-sm font-semibold text-slate-500 uppercase">Execuções Totais</p>
            <CheckCircle2 size={20} className="text-blue-500" />
          </div>
          <div className="mt-4">
            <span className="text-4xl font-extrabold text-slate-900">{metrics.totalExecutions}</span>
            <p className="text-sm text-slate-400 mt-1">Registadas na pipeline</p>
          </div>
        </div>

        <div className="bg-white rounded-xl border border-red-100 p-6 shadow-sm bg-gradient-to-br from-white to-red-50/30">
          <div className="flex justify-between items-start">
            <p className="text-sm font-semibold text-red-600 uppercase">Testes Flaky</p>
            <ShieldAlert size={20} className="text-red-500" />
          </div>
          <div className="mt-4">
            <span className="text-4xl font-extrabold text-red-700">{metrics.totalFlaky}</span>
            <p className="text-sm text-red-500/70 mt-1 font-medium">Instabilidades detetadas</p>
          </div>
        </div>
      </div>

      {/* GRÁFICO: Análise Historial de Performance */}
      <div className="bg-white rounded-xl border border-slate-200 shadow-sm p-6 mb-8 min-h-[400px]">
        <div className="flex items-center justify-between mb-8">
          <div className="flex items-center gap-2">
            <div className="p-2 bg-blue-50 rounded-lg text-blue-600">
              <TrendingUp size={20} />
            </div>
            <h3 className="text-lg font-bold text-slate-900">Análise de Tendência de Execução</h3>
          </div>
          <span className="text-xs font-medium text-slate-400 uppercase tracking-widest px-3 py-1 bg-slate-50 rounded-full border border-slate-100">
            Últimas 10 Builds
          </span>
        </div>
        
        {/* Altura definida explicitamente na div pai (h-80 ou 320px) */}
        <div style={{ width: '100%', height: 320, minWidth: 0 }}>
          {historyData.length > 0 ? (
            <ResponsiveContainer width="100%" height={320}>
              <AreaChart data={historyData}>
                <defs>
                  <linearGradient id="colorDuration" x1="0" y1="0" x2="0" y2="1">
                    <stop offset="5%" stopColor="#3b82f6" stopOpacity={0.1}/>
                    <stop offset="95%" stopColor="#3b82f6" stopOpacity={0}/>
                  </linearGradient>
                </defs>
                <CartesianGrid strokeDasharray="3 3" vertical={false} stroke="#f1f5f9" />
                <XAxis dataKey="name" tick={{fill: '#94a3b8', fontSize: 12}} dy={10} />
                <YAxis tick={{fill: '#94a3b8', fontSize: 12}} unit="m" />
                <Tooltip contentStyle={{ borderRadius: '12px', border: 'none', boxShadow: '0 10px 15px -3px rgb(0 0 0 / 0.1)' }} />
                <Area type="monotone" dataKey="duration" name="Duração (min)" stroke="#3b82f6" strokeWidth={3} fill="url(#colorDuration)" />
              </AreaChart>
            </ResponsiveContainer>
          ) : (
            <div className="flex items-center justify-center h-full text-slate-400 italic">
              Aguardando dados históricos para gerar o gráfico...
            </div>
          )}
        </div>
      </div>

      {/* Tabelas de Detalhes Críticos */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-8">
        
        {/* Lista de Falhas Recentes vindas do Backend */}
        <div className="bg-white rounded-xl border border-slate-200 shadow-sm overflow-hidden flex flex-col">
          <div className="px-6 py-4 border-b border-slate-200 flex items-center gap-2">
            <XCircle size={18} className="text-red-500" />
            <h3 className="font-bold text-slate-800 text-sm uppercase tracking-wider">Falhas Críticas (Última Run)</h3>
          </div>
          <div className="overflow-x-auto">
            <table className="min-w-full divide-y divide-slate-100">
              <tbody className="divide-y divide-slate-100">
                {metrics.recentFailures.map((test) => (
                  <tr key={test.id} className="hover:bg-red-50/30 transition-colors">
                    <td className="px-6 py-4 text-xs font-mono text-slate-600 break-all">
                      {test.name}
                    </td>
                    <td className="px-6 py-4 text-right">
                      <span className="text-[10px] font-bold uppercase px-2 py-1 bg-red-100 text-red-600 rounded">
                        {test.status}
                      </span>
                    </td>
                  </tr>
                ))}
                {metrics.recentFailures.length === 0 && (
                  <tr>
                    <td className="px-6 py-12 text-center text-sm text-slate-400 italic">
                      Nenhuma falha detetada na execução mais recente.
                    </td>
                  </tr>
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
              <tbody className="divide-y divide-slate-100">
                {metrics.flakyTests.map((test) => (
                  <tr key={test.id} className="hover:bg-amber-50/30 transition-colors">
                    <td className="px-6 py-4 text-xs font-mono text-slate-600 break-all">
                      {test.name}
                    </td>
                    <td className="px-6 py-4 text-right">
                      <div className="flex flex-col items-end">
                        <span className="text-[10px] font-bold text-amber-600 uppercase">Risco {test.failureRate}</span>
                        <div className="w-16 bg-slate-100 h-1 mt-1 rounded-full overflow-hidden">
                          <div className="bg-amber-400 h-full w-2/3"></div>
                        </div>
                      </div>
                    </td>
                  </tr>
                ))}
                {metrics.flakyTests.length === 0 && (
                  <tr>
                    <td className="px-6 py-12 text-center text-sm text-slate-400 italic">
                      Ainda não foram detetados testes com comportamento instável.
                    </td>
                  </tr>
                )}
              </tbody>
            </table>
          </div>
        </div>

      </div>
    </Layout>
  );
}