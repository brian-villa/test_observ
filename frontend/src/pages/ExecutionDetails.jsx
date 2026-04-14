import React, { useState, useEffect } from 'react';
import { useParams, Link } from 'react-router-dom';
import Layout from '../components/Layout';
import { 
  ArrowLeft, Activity, AlertTriangle, FileCode2, 
  Search, Filter, ChevronDown, ChevronRight, 
  CheckCircle2, XCircle
} from 'lucide-react';
import api from '../services/api';
import toast from 'react-hot-toast';

export default function ExecutionDetails() {
  const { projectId, executionId } = useParams();
  
  const [isLoading, setIsLoading] = useState(true);
  const [testResults, setTestResults] = useState([]);

  const [searchTerm, setSearchTerm] = useState('');
  const [statusFilter, setStatusFilter] = useState('ALL');
  const [showOnlyFlaky, setShowOnlyFlaky] = useState(false);

  const [expandedRowId, setExpandedRowId] = useState(null);

  useEffect(() => {
    const fetchExecutionDetails = async () => {
      try {
        setIsLoading(true);
        
        const params = new URLSearchParams();
        if (searchTerm) params.append('search', searchTerm);
        params.append('status', statusFilter);
        params.append('flakyOnly', showOnlyFlaky);
        params.append('size', '2000'); // Limite seguro sem paginação no frontend

        const res = await api.get(`/executions/${executionId}/results?${params.toString()}`);
        setTestResults(res.data.content || []);
        
      } catch (error) {
        console.error("Erro ao buscar detalhes da execução:", error);
        toast.error("Não foi possível carregar os detalhes da execução.");
      } finally {
        setIsLoading(false);
      }
    };

    if (executionId) fetchExecutionDetails();
  }, [executionId, searchTerm, statusFilter, showOnlyFlaky]);

  useEffect(() => {
    setExpandedRowId(null);
  }, [searchTerm, statusFilter, showOnlyFlaky]);

  const toggleRow = (id) => {
    setExpandedRowId(expandedRowId === id ? null : id);
  };

  return (
    <Layout>
      {/* ── Cabeçalho ── */}
      <div className="mb-6 flex flex-col md:flex-row md:items-center justify-between gap-4">
        <div>
          <Link to={`/projects/${projectId}`} className="inline-flex items-center text-sm font-medium text-slate-500 mb-4 hover:text-slate-700 transition-colors">
            <ArrowLeft size={16} className="mr-1" /> Voltar ao Dashboard
          </Link>
          <div className="flex items-center gap-3">
            <FileCode2 className="text-blue-500" size={28} />
            <h1 className="text-3xl font-bold text-slate-900">Relatório da Execução</h1>
          </div>
        </div>

        {/* Filtros Compactos (Estilo ProjectDetails) */}
        <div className="flex flex-col sm:flex-row items-center gap-3 bg-white p-3 rounded-xl border border-slate-200 shadow-sm">
          <div className="relative flex-1 sm:w-48">
            <Search className="absolute left-2.5 top-1/2 -translate-y-1/2 text-slate-400" size={14} />
            <input 
              type="text" 
              placeholder="Procurar teste..." 
              value={searchTerm}
              onChange={(e) => setSearchTerm(e.target.value)}
              className="w-full pl-8 pr-3 py-1.5 text-xs bg-slate-50 border border-slate-200 rounded-lg focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-blue-500 transition-all"
            />
          </div>

          <div className="flex items-center gap-2 border-l border-slate-100 pl-3">
            <Filter size={14} className="text-slate-400" />
            <select 
              value={statusFilter} 
              onChange={(e) => setStatusFilter(e.target.value)}
              className="bg-slate-50 border border-slate-200 text-slate-700 text-xs rounded-lg px-2 py-1.5 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-blue-500 cursor-pointer"
            >
              <option value="ALL">Todos os Status</option>
              <option value="PASS">PASS</option>
              <option value="FAIL">FAIL</option>
            </select>
          </div>

          <label className="flex items-center gap-1.5 cursor-pointer ml-1 border-l border-slate-100 pl-3">
            <input 
              type="checkbox" 
              checked={showOnlyFlaky}
              onChange={(e) => setShowOnlyFlaky(e.target.checked)}
              className="rounded text-amber-500 focus:ring-amber-500 border-slate-300 w-3.5 h-3.5"
            />
            <span className="text-xs font-semibold text-slate-600 flex items-center gap-1">
              <AlertTriangle size={12} className="text-amber-500" /> Somente Flaky
            </span>
          </label>
          
          {(searchTerm || statusFilter !== 'ALL' || showOnlyFlaky) && (
            <button onClick={() => { setSearchTerm(''); setStatusFilter('ALL'); setShowOnlyFlaky(false); }}
              className="text-[10px] text-blue-600 hover:underline whitespace-nowrap ml-2 font-bold uppercase tracking-wider">
              Limpar
            </button>
          )}
        </div>
      </div>

      {/* ── Tabela de Resultados ── */}
      <div className="bg-white rounded-xl border border-slate-200 shadow-sm overflow-hidden flex flex-col min-h-[400px]">
        {/* Header da Tabela com total */}
        <div className="px-6 py-3 border-b border-slate-100 bg-slate-50/50 flex items-center justify-between">
          <h3 className="font-bold text-slate-800 text-xs uppercase tracking-wider">Casos de Teste</h3>
          <span className="text-[10px] font-bold px-2 py-0.5 rounded-full bg-slate-200 text-slate-600">
            {testResults.length} {testResults.length === 1 ? 'teste' : 'testes'}
          </span>
        </div>

        {isLoading ? (
          <div className="flex justify-center items-center flex-1 h-64">
            <Activity className="text-blue-500 animate-spin" size={32} />
          </div>
        ) : testResults.length > 0 ? (
          <div className="overflow-x-auto flex-1">
            <table className="min-w-full divide-y divide-slate-100">
              <thead className="bg-white">
                <tr>
                  <th className="w-8 px-4 py-3"></th>
                  <th className="px-4 py-3 text-left text-[10px] font-bold text-slate-400 uppercase tracking-wider">Nome do Teste</th>
                  <th className="px-4 py-3 text-center text-[10px] font-bold text-slate-400 uppercase tracking-wider w-24">Flaky</th>
                  <th className="px-6 py-3 text-right text-[10px] font-bold text-slate-400 uppercase tracking-wider w-32">Status</th>
                </tr>
              </thead>
              <tbody className="bg-white divide-y divide-slate-100">
                {testResults.map((test) => (
                  <React.Fragment key={test.id}>
                    {/* LINHA PRINCIPAL */}
                    <tr 
                      onClick={() => toggleRow(test.id)}
                      className={`hover:bg-slate-50/60 transition-colors cursor-pointer group ${expandedRowId === test.id ? 'bg-blue-50/20' : ''}`}
                    >
                      <td className="px-4 py-2.5 text-slate-300 group-hover:text-blue-400 transition-colors">
                        {expandedRowId === test.id ? <ChevronDown size={16} /> : <ChevronRight size={16} />}
                      </td>
                      <td className="px-4 py-2.5 text-xs font-mono text-slate-600 break-all group-hover:text-blue-700 transition-colors">
                        {test.testCaseName}
                      </td>
                      <td className="px-4 py-2.5 text-center">
                        {test.isFlaky && <AlertTriangle size={14} className="text-amber-500 mx-auto" />}
                      </td>
                      <td className="px-6 py-2.5 text-right">
                        <span className={`inline-flex items-center justify-center gap-1 w-20 py-1 rounded-md text-[10px] font-bold uppercase tracking-widest ${
                          test.result === 'PASS' ? 'bg-green-100 text-green-700' : 
                          test.result === 'FAIL' ? 'bg-red-100 text-red-600' : 'bg-slate-100 text-slate-600'
                        }`}>
                          {test.result === 'PASS' && <CheckCircle2 size={10} />}
                          {test.result === 'FAIL' && <XCircle size={10} />}
                          {test.result}
                        </span>
                      </td>
                    </tr>

                    {/* GAVETA DE DETALHES */}
                    {expandedRowId === test.id && (
                      <tr className="bg-slate-50/50 border-b border-slate-100 shadow-inner">
                        <td colSpan="4" className="px-8 py-4">
                          <div className="flex flex-col gap-3">
                            
                            {/* AVISO FLAKY */}
                            {test.isFlaky && (
                              <div className="border-l-2 border-amber-400 bg-amber-50/50 px-3 py-2 rounded-r-md flex items-start gap-2">
                                <AlertTriangle size={14} className="text-amber-500 mt-0.5 shrink-0" />
                                <div>
                                  <span className="text-amber-800 font-bold text-[11px] uppercase tracking-wider block mb-0.5">Alerta de Instabilidade</span>
                                  <p className="text-[11px] text-amber-700 leading-relaxed">
                                    {test.flakyReason || "Este teste excedeu o limite aceitável de falhas intermitentes nesta pipeline."}
                                  </p>
                                </div>
                              </div>
                            )}

                            {/* DETALHE DE FALHA */}
                            {test.result === 'FAIL' && (
                              <div className="flex items-start gap-2 bg-red-50/50 px-3 py-2.5 rounded-md border border-red-100">
                                <XCircle className="text-red-500 mt-0.5 shrink-0" size={14} />
                                <div className="w-full overflow-hidden">
                                  <h4 className="font-bold text-red-800 text-[11px] uppercase tracking-wider mb-1.5">Motivo da Falha</h4>
                                  {test.stackTrace ? (
                                    <pre className="bg-white p-2.5 rounded border border-red-100 text-[11px] text-red-700 font-mono overflow-x-auto whitespace-pre-wrap leading-relaxed">
                                      {test.stackTrace}
                                    </pre>
                                  ) : (
                                    <p className="text-[11px] text-red-600 italic">
                                      A execução falhou, mas o runner não exportou a stacktrace. Consulte o servidor de CI.
                                    </p>
                                  )}
                                </div>
                              </div>
                            )}

                            {/* MENSAGEM DE SUCESSO */}
                            {test.result === 'PASS' && !test.isFlaky && (
                              <div className="text-[11px] text-slate-400 italic flex items-center gap-1.5">
                                <CheckCircle2 size={12} className="text-green-500" />
                                Executado com sucesso.
                              </div>
                            )}
                          </div>
                        </td>
                      </tr>
                    )}
                  </React.Fragment>
                ))}
              </tbody>
            </table>
          </div>
        ) : (
          <div className="flex flex-col items-center justify-center py-16 gap-3 text-slate-400">
            <FileCode2 size={32} className="text-slate-200" />
            <p className="text-xs italic">Nenhum teste encontrado para os filtros atuais.</p>
          </div>
        )}
      </div>
    </Layout>
  );
}