import React, { useState, useEffect } from 'react';
import { useParams, Link } from 'react-router-dom';
import Layout from '../components/Layout';
import { 
  ArrowLeft, Activity, AlertTriangle, FileCode2, 
  Search, Filter, ChevronDown, ChevronRight, 
  CheckCircle2, XCircle, Info 
} from 'lucide-react';
import api from '../services/api';
import toast from 'react-hot-toast';

export default function ExecutionDetails() {
  const { projectId, executionId } = useParams();
  
  // Estados dos Dados
  const [isLoading, setIsLoading] = useState(true);
  const [testResults, setTestResults] = useState([]);
  const [totalItems, setTotalItems] = useState(0);
  const [totalPages, setTotalPages] = useState(1);

  // Estados dos Filtros
  const [searchTerm, setSearchTerm] = useState('');
  const [statusFilter, setStatusFilter] = useState('ALL');
  const [showOnlyFlaky, setShowOnlyFlaky] = useState(false);

  // Estados de Paginação e UX
  const [pageSize, setPageSize] = useState(50);
  const [currentPage, setCurrentPage] = useState(1);
  const [expandedRowId, setExpandedRowId] = useState(null);

  // Efeito principal: Busca os dados no Backend sempre que um filtro ou página muda
  useEffect(() => {
    const fetchExecutionDetails = async () => {
      try {
        setIsLoading(true);
        
        const params = new URLSearchParams();
        if (searchTerm) params.append('search', searchTerm);
        params.append('status', statusFilter);
        params.append('flakyOnly', showOnlyFlaky);
        
        // No Spring, a página começa em 0
        params.append('page', currentPage - 1); 
        params.append('size', pageSize === 'ALL' ? 5000 : pageSize);

        const res = await api.get(`/executions/${executionId}/results?${params.toString()}`);
        
        setTestResults(res.data.content || []);
        setTotalItems(res.data.totalElements || 0);
        setTotalPages(res.data.totalPages || 1);
        
      } catch (error) {
        console.error("Erro ao buscar detalhes da execução:", error);
        toast.error("Não foi possível carregar os detalhes da execução.");
      } finally {
        setIsLoading(false);
      }
    };

    if (executionId) fetchExecutionDetails();
  }, [executionId, searchTerm, statusFilter, showOnlyFlaky, currentPage, pageSize]);

  // Se o utilizador mudar os filtros, volta à página 1 e fecha o acordeão
  useEffect(() => {
    setCurrentPage(1);
    setExpandedRowId(null);
  }, [searchTerm, statusFilter, showOnlyFlaky, pageSize]);

  // Controla a abertura da gaveta de detalhes do teste
  const toggleRow = (id) => {
    setExpandedRowId(expandedRowId === id ? null : id);
  };

  return (
    <Layout>
      <div className="mb-6">
        <Link to={`/projects/${projectId}`} className="inline-flex items-center text-sm font-medium text-slate-500 mb-4 hover:text-slate-700 transition-colors">
          <ArrowLeft size={16} className="mr-1" /> Voltar ao Dashboard do Projeto
        </Link>
        <h1 className="text-3xl font-bold text-slate-900 flex items-center gap-3">
          <FileCode2 className="text-blue-500" size={32} />
          Relatório da Execução
        </h1>
      </div>

      {/* BARRA DE FERRAMENTAS E FILTROS */}
      <div className="bg-white p-4 rounded-xl border border-slate-200 shadow-sm mb-6 flex flex-col md:flex-row gap-4 items-center justify-between">
        <div className="flex flex-1 w-full gap-4 items-center">
          
          <div className="relative flex-1 max-w-md">
            <Search className="absolute left-3 top-1/2 -translate-y-1/2 text-slate-400" size={18} />
            <input 
              type="text" 
              placeholder="Procurar caso de teste..." 
              value={searchTerm}
              onChange={(e) => setSearchTerm(e.target.value)}
              className="w-full pl-10 pr-4 py-2 text-sm border border-slate-200 rounded-lg focus:ring-2 focus:ring-blue-500 focus:outline-none"
            />
          </div>

          <div className="flex items-center gap-2 border-l border-slate-200 pl-4">
            <Filter className="text-slate-400" size={16} />
            <select 
              value={statusFilter} 
              onChange={(e) => setStatusFilter(e.target.value)}
              className="text-sm border-none bg-transparent focus:ring-0 cursor-pointer text-slate-700 font-medium outline-none"
            >
              <option value="ALL">Todos os Status</option>
              <option value="PASS">PASS</option>
              <option value="FAIL">FAIL</option>
            </select>
          </div>

          <label className="flex items-center gap-2 cursor-pointer ml-4">
            <input 
              type="checkbox" 
              checked={showOnlyFlaky}
              onChange={(e) => setShowOnlyFlaky(e.target.checked)}
              className="rounded text-amber-500 focus:ring-amber-500 border-slate-300"
            />
            <span className="text-sm font-medium text-slate-700 flex items-center gap-1">
              <AlertTriangle size={14} className="text-amber-500" /> Somente Flaky
            </span>
          </label>
        </div>

        <div className="flex items-center gap-2 text-sm text-slate-500">
          Mostrar:
          <select 
            value={pageSize} 
            onChange={(e) => setPageSize(e.target.value === 'ALL' ? 'ALL' : Number(e.target.value))}
            className="border border-slate-200 rounded px-2 py-1 bg-slate-50 focus:outline-none focus:border-blue-500"
          >
            <option value={20}>20</option>
            <option value={50}>50</option>
            <option value={100}>100</option>
            <option value={200}>200</option>
            <option value="ALL">Todos</option>
          </select>
        </div>
      </div>

      {/* TABELA DE RESULTADOS */}
      <div className="bg-white rounded-xl border border-slate-200 shadow-sm overflow-hidden flex flex-col min-h-400px">
        {isLoading ? (
          <div className="flex justify-center items-center h-64 flex-1">
            <Activity className="text-blue-500 animate-spin" size={40} />
          </div>
        ) : testResults.length > 0 ? (
          <div className="overflow-x-auto flex-1">
            <table className="min-w-full divide-y divide-slate-200">
              <thead className="bg-slate-50">
                <tr>
                  <th className="w-10 px-4 py-4"></th>
                  <th className="px-4 py-4 text-left text-xs font-semibold text-slate-500 uppercase tracking-wider">Caso de Teste</th>
                  <th className="px-4 py-4 text-center text-xs font-semibold text-slate-500 uppercase tracking-wider">Flaky</th>
                  <th className="px-6 py-4 text-right text-xs font-semibold text-slate-500 uppercase tracking-wider">Status Final</th>
                </tr>
              </thead>
              <tbody className="bg-white divide-y divide-slate-100">
                {testResults.map((test) => (
                  <React.Fragment key={test.id}>
                    {/* LINHA PRINCIPAL */}
                    <tr 
                      onClick={() => toggleRow(test.id)}
                      className={`hover:bg-slate-50 transition-colors cursor-pointer group ${expandedRowId === test.id ? 'bg-blue-50/30' : ''}`}
                    >
                      <td className="px-4 py-4 text-slate-400">
                        {expandedRowId === test.id ? <ChevronDown size={18} /> : <ChevronRight size={18} />}
                      </td>
                      <td className="px-4 py-4 text-sm font-mono text-slate-700 break-all group-hover:text-blue-600 transition-colors">
                        {test.testCaseName}
                      </td>
                      <td className="px-4 py-4 text-center">
                        {test.isFlaky && <AlertTriangle size={16} className="text-amber-500 mx-auto" />}
                      </td>
                      <td className="px-6 py-4 text-right">
                        <span className={`inline-flex items-center gap-1 px-3 py-1 rounded-full text-[11px] font-bold uppercase tracking-widest ${
                          test.result === 'PASS' ? 'bg-green-100 text-green-700' : 
                          test.result === 'FAIL' ? 'bg-red-100 text-red-700' : 'bg-slate-100 text-slate-700'
                        }`}>
                          {test.result === 'PASS' && <CheckCircle2 size={12} />}
                          {test.result === 'FAIL' && <XCircle size={12} />}
                          {test.result}
                        </span>
                      </td>
                    </tr>

                    {/* GAVETA DE DETALHES (Acordeão) */}
                    {expandedRowId === test.id && (
                      <tr className="bg-slate-50/50 border-b border-slate-200">
                        <td colSpan="4" className="px-8 py-6">
                          <div className="flex flex-col gap-4">
                            
                            {test.isFlaky && (
                              <div className="mb-3 border-l-2 border-amber-500 pl-3">
                                <span className="text-amber-500 font-bold">[WARN] Flaky Test Detected:</span> 
                                <span className="ml-2 text-slate-300">
                                  {test.flakyReason || "Este teste tem demonstrado instabilidade no histórico da branch."}
                                </span>
                              </div>
                            )}

                            {test.result === 'FAIL' && (
                              <div className="flex gap-3 bg-red-50 p-4 rounded-lg border border-red-200">
                                <XCircle className="text-red-600 shrink-0 mt-0.5" size={20} />
                                <div className="w-full overflow-hidden">
                                  <h4 className="font-bold text-red-800 text-sm mb-2">Detalhes da Falha</h4>
                                  <pre className="bg-white p-3 rounded border border-red-100 text-xs text-red-700 font-mono overflow-x-auto">
                                    Sem stacktrace disponível.
                                  </pre>
                                </div>
                              </div>
                            )}

                            {test.result === 'PASS' && !test.isFlaky && (
                              <div className="text-sm text-slate-500 italic flex items-center gap-2">
                                <CheckCircle2 size={16} className="text-green-500" />
                                Este caso de teste executou sem erros.
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
          <div className="text-center py-20 text-slate-400 italic flex-1 flex flex-col items-center justify-center">
            <Search size={48} className="text-slate-200 mb-4" />
            <p>Nenhum teste encontrado para os filtros atuais.</p>
            <button 
              onClick={() => { setSearchTerm(''); setStatusFilter('ALL'); setShowOnlyFlaky(false); }}
              className="mt-4 text-blue-500 hover:underline text-sm"
            >
              Limpar Filtros
            </button>
          </div>
        )}

        {/* Paginador */}
        {pageSize !== 'ALL' && totalPages > 1 && (
          <div className="px-6 py-4 border-t border-slate-100 bg-white flex items-center justify-between">
            <p className="text-sm text-slate-500">
              A mostrar <span className="font-medium text-slate-900">{(currentPage - 1) * pageSize + 1}</span> a <span className="font-medium text-slate-900">{Math.min(currentPage * pageSize, totalItems)}</span> de <span className="font-medium text-slate-900">{totalItems}</span> resultados
            </p>
            <div className="flex gap-2">
              <button 
                onClick={() => setCurrentPage(p => Math.max(1, p - 1))}
                disabled={currentPage === 1}
                className="px-3 py-1 border border-slate-200 rounded text-sm text-slate-600 disabled:opacity-50 disabled:bg-slate-50 hover:bg-slate-50"
              >
                Anterior
              </button>
              <button 
                onClick={() => setCurrentPage(p => Math.min(totalPages, p + 1))}
                disabled={currentPage === totalPages}
                className="px-3 py-1 border border-slate-200 rounded text-sm text-slate-600 disabled:opacity-50 disabled:bg-slate-50 hover:bg-slate-50"
              >
                Próxima
              </button>
            </div>
          </div>
        )}
      </div>
    </Layout>
  );
}