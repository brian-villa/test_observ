import React, { useState, useEffect } from 'react';
import { useParams, Link, useLocation } from 'react-router-dom';
import Layout from '../components/Layout';
import { 
  ArrowLeft, Activity, AlertTriangle, GitMerge, 
  Search, Filter, ChevronDown, ChevronRight, 
  CheckCircle2, XCircle, FileCode2
} from 'lucide-react';
import api from '../services/api';
import toast from 'react-hot-toast';

export default function ExecutionDetails() {
  const { projectId, executionId } = useParams();
  const location = useLocation();
  
  const [isLoading, setIsLoading] = useState(true);
  const [testResults, setTestResults] = useState([]);
  
  // Título
  const [buildName, setBuildName] = useState(location.state?.buildName || 'A carregar build...');

  const [searchTerm, setSearchTerm] = useState(location.state?.search || '');
  const [statusFilter, setStatusFilter] = useState(location.state?.status || 'ALL');
  const [showOnlyFlaky, setShowOnlyFlaky] = useState(location.state?.flakyOnly || false);

  const [expandedRowId, setExpandedRowId] = useState(null);

  const normalizeTestName = (fullName) => {
    if (!fullName) return "Teste Desconhecido";
    const parts = fullName.split('.');
    return parts.length > 2 ? parts.slice(-2).join('.') : fullName;
  };

  useEffect(() => {
    // Função de resgate: Se o utilizador der F5 e perder o state da rota, o componente descobre o nome sozinho
    const fetchBuildNameFallback = async () => {
      if (!location.state?.buildName) {
        try {
          const historyRes = await api.get(`/api/v1/projects/${projectId}/dashboard/history?size=100`);
          const exec = historyRes.data.content?.find(e => e.id === executionId || e.executionId === executionId);
          if (exec && exec.versionName && exec.versionName !== 'N/A') {
            setBuildName(exec.versionName);
          } else {
            setBuildName(`Build ${executionId.substring(0, 8)}`);
          }
        } catch (e) {
          setBuildName('Detalhes da Execução');
        }
      }
    };

    const fetchExecutionDetails = async () => {
      try {
        setIsLoading(true);
        fetchBuildNameFallback();
        
        const params = new URLSearchParams();
        if (searchTerm) params.append('search', searchTerm);
        params.append('status', statusFilter);
        params.append('flakyOnly', showOnlyFlaky);
        params.append('size', '2000'); 

        const res = await api.get(`/executions/${executionId}/results?${params.toString()}`);
        setTestResults(res.data.content || res.data || []);
        
      } catch (error) {
        console.error("Erro ao buscar detalhes da execução:", error);
        toast.error("Não foi possível carregar a lista de testes.");
      } finally {
        setIsLoading(false);
      }
    };

    if (executionId) fetchExecutionDetails();
  }, [executionId, searchTerm, statusFilter, showOnlyFlaky, projectId, location.state]);

  useEffect(() => {
    setExpandedRowId(null);
  }, [searchTerm, statusFilter, showOnlyFlaky]);

  const toggleRow = (id, canExpand) => {
    if (!canExpand) return;
    setExpandedRowId(expandedRowId === id ? null : id);
  };

  const counts = testResults.reduce((acc, test) => {
    acc.total++;
    if (test.result === 'PASS') acc.pass++;
    else if (test.result === 'FAIL') acc.fail++;
    if (test.isFlaky) acc.flaky++;
    return acc;
  }, { total: 0, pass: 0, fail: 0, skip: 0, flaky: 0 });

  return (
    <Layout>
      <div className="mb-6 flex flex-col md:flex-row md:items-end justify-between gap-4">
        <div>
          <Link to={`/projects/${projectId}`} className="inline-flex items-center text-sm font-medium text-slate-500 mb-4 hover:text-slate-700 transition-colors">
            <ArrowLeft size={16} className="mr-1" /> Voltar ao Dashboard
          </Link>
          <div className="flex items-center gap-3">
            <GitMerge className="text-blue-500" size={28} />
            <h1 className="text-3xl font-bold text-slate-900">{buildName}</h1>
          </div>
        </div>

        <div className="flex flex-col sm:flex-row items-center gap-3 bg-white p-2 rounded-xl border border-slate-200 shadow-sm">
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

          <label className="flex items-center gap-1.5 cursor-pointer ml-1 border-l border-slate-100 pl-3 pr-2">
            <input 
              type="checkbox" 
              checked={showOnlyFlaky}
              onChange={(e) => setShowOnlyFlaky(e.target.checked)}
              className="rounded text-amber-500 focus:ring-amber-500 border-slate-300 w-3.5 h-3.5 cursor-pointer"
            />
            <span className="text-xs font-semibold text-slate-600 flex items-center gap-1">
              <AlertTriangle size={12} className="text-amber-500" /> Flaky
            </span>
          </label>
        </div>
      </div>

      <div className="bg-white rounded-xl border border-slate-200 shadow-sm overflow-hidden flex flex-col min-h-[400px]">
        
        {!isLoading && testResults.length > 0 && (
          <div className="px-6 py-3 border-b border-slate-100 bg-slate-50/50 flex flex-wrap items-center gap-4 text-xs">
            <span className="font-bold text-slate-800 uppercase tracking-wider mr-2">Casos de Teste: {counts.total}</span>
            <span className="flex items-center gap-1 font-semibold text-green-700 bg-green-100 px-2 py-0.5 rounded-md"><CheckCircle2 size={12}/> Pass: {counts.pass}</span>
            <span className="flex items-center gap-1 font-semibold text-red-700 bg-red-100 px-2 py-0.5 rounded-md"><XCircle size={12}/> Fail: {counts.fail}</span>
            {counts.flaky > 0 && <span className="flex items-center gap-1 font-semibold text-amber-700 bg-amber-100 px-2 py-0.5 rounded-md ml-auto"><AlertTriangle size={12}/> Flaky: {counts.flaky}</span>}
          </div>
        )}

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
                {testResults.map((test) => {
                  const isFail = test.result === 'FAIL';
                  const isPass = test.result === 'PASS';
                  const canExpand = isFail || test.isFlaky;

                  const fullName = test.testCaseName || test.name || "Teste Desconhecido";

                  return (
                    <React.Fragment key={test.id}>
                      <tr 
                        onClick={() => toggleRow(test.id, canExpand)}
                        className={`transition-colors group ${
                          canExpand ? 'hover:bg-slate-50/60 cursor-pointer' : 'cursor-default'
                        } ${expandedRowId === test.id ? 'bg-blue-50/20' : ''}`}
                      >
                        <td className="px-4 py-2.5 text-slate-300">
                          {canExpand && (
                            <span className="group-hover:text-blue-400 transition-colors">
                              {expandedRowId === test.id ? <ChevronDown size={16} /> : <ChevronRight size={16} />}
                            </span>
                          )}
                        </td>
                        <td className={`px-4 py-2.5 text-xs font-mono text-slate-600 break-all transition-colors ${canExpand ? 'group-hover:text-blue-700' : ''}`} title={fullName}>
                          {normalizeTestName(fullName)}
                        </td>
                        <td className="px-4 py-2.5 text-center">
                          {test.isFlaky && <AlertTriangle size={14} className="text-amber-500 mx-auto" />}
                        </td>
                        <td className="px-6 py-2.5 text-right">
                          <span className={`inline-flex items-center justify-center gap-1 w-20 py-1 rounded-md text-[10px] font-bold uppercase tracking-widest ${
                            isPass ? 'bg-green-100 text-green-700' : 
                            isFail ? 'bg-red-100 text-red-600' : 
                            'bg-slate-100 text-slate-600'
                          }`}>
                            {isPass && <CheckCircle2 size={10} />}
                            {isFail && <XCircle size={10} />}
                            {test.result}
                          </span>
                        </td>
                      </tr>

                      {expandedRowId === test.id && canExpand && (
                        <tr className="bg-slate-50/50 border-b border-slate-100 shadow-inner">
                          <td colSpan="4" className="px-8 py-4">
                            <div className="flex flex-col gap-3">
                              
                              {test.isFlaky && (
                                <div className="border-l-2 border-amber-400 bg-amber-50/50 px-3 py-2 rounded-r-md flex items-start gap-2">
                                  <AlertTriangle size={14} className="text-amber-500 mt-0.5 shrink-0" />
                                  <div>
                                    <span className="text-amber-800 font-bold text-[11px] uppercase tracking-wider block mb-0.5">Alerta de Instabilidade</span>
                                    <p className="text-[11px] text-amber-700 leading-relaxed">
                                      {test.flakyReason || "Este teste demonstrou instabilidade no histórico recente desta pipeline."}
                                    </p>
                                  </div>
                                </div>
                              )}

                              {isFail && (
                                <div className="flex items-start gap-2 bg-red-50/50 px-3 py-2.5 rounded-md border border-red-100">
                                  <XCircle className="text-red-500 mt-0.5 shrink-0" size={14} />
                                  <div className="w-full overflow-hidden">
                                    <h4 className="font-bold text-red-800 text-[11px] uppercase tracking-wider mb-1.5">Detalhes da Falha</h4>
                                    {test.stackTrace || test.errorMessage ? (
                                      <pre className="bg-white p-2.5 rounded border border-red-100 text-[11px] text-red-700 font-mono overflow-x-auto whitespace-pre-wrap leading-relaxed">
                                        {test.stackTrace || test.errorMessage}
                                      </pre>
                                    ) : (
                                      <p className="text-[11px] text-red-600 italic">
                                        O teste falhou, mas a infraestrutura de CI/CD não forneceu o motivo do erro.
                                      </p>
                                    )}
                                  </div>
                                </div>
                              )}

                            </div>
                          </td>
                        </tr>
                      )}
                    </React.Fragment>
                  );
                })}
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