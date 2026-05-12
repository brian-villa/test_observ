import React, { useState, useEffect } from 'react';
import { useParams, Link, useLocation } from 'react-router-dom';
import Layout from '../components/Layout';
import { 
  ArrowLeft, Activity, AlertTriangle, GitMerge, 
  Search, Filter, ChevronDown, ChevronRight, 
  CheckCircle2, XCircle, FileCode2, Image as ImageIcon
} from 'lucide-react';
import api from '../services/api';
import toast from 'react-hot-toast';

export default function ExecutionDetails() {
  const { projectId, executionId } = useParams();
  const location = useLocation();
  
  const [isLoading, setIsLoading] = useState(true);
  const [testResults, setTestResults] = useState([]);
  
  const [buildName, setBuildName] = useState(location.state?.buildName || 'A carregar build...');

  const [searchTerm, setSearchTerm] = useState(location.state?.search || '');
  const [statusFilter, setStatusFilter] = useState(location.state?.status || 'ALL');
  const [showOnlyFlaky, setShowOnlyFlaky] = useState(location.state?.flakyOnly || false);
  const [expandedRowId, setExpandedRowId] = useState(null);

  // Modal para imagem
  const [fullscreenImage, setFullscreenImage] = useState(null);

  const normalizeTestName = (fullName) => {
    if (!fullName) return "Teste Desconhecido";
    const parts = fullName.split('.');
    return parts.length > 2 ? parts.slice(-2).join('.') : fullName;
  };

  useEffect(() => {
    const fetchBuildNameFallback = async () => {
      if (!location.state?.buildName) {
        try {
          const historyRes = await api.get(`/api/v1/projects/${projectId}/dashboard/history?size=100`);
          const exec = historyRes.data.content?.find(e => e.id === executionId || e.executionId === executionId);
          if (exec && exec.buildName) {
            setBuildName(exec.buildName);
          } else if (exec && exec.versionName && exec.versionName !== 'N/A') {
            setBuildName(`Build da Versão ${exec.versionName}`);
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

        const idsToFetch = location.state?.executionIds || [executionId];
        
        const promises = idsToFetch.map(id => api.get(`/executions/${id}/results?${params.toString()}`));
        const responses = await Promise.all(promises);

        const combinedResults = responses.flatMap(res => res.data.content || res.data || []);

        // Remove duplicates from retries
        const uniqueResults = Array.from(
          new Map(combinedResults.map(item => [item.testCaseName || item.name, item])).values()
        );
        
        setTestResults(uniqueResults);
        
      } catch (error) {
        toast.error("Não foi possível carregar a lista completa de testes.");
      } finally {
        setIsLoading(false);
      }
    };

    if (executionId) fetchExecutionDetails();
  }, [executionId, searchTerm, statusFilter, showOnlyFlaky, projectId, location.state]);

  useEffect(() => { setExpandedRowId(null); }, [searchTerm, statusFilter, showOnlyFlaky]);

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
      {/* MODAL DE IMAGEM FULLSCREEN DINÂMICO (JPEG ou PNG) */}
      {fullscreenImage && (
        <div 
          className="fixed inset-0 z-50 bg-slate-900/90 flex items-center justify-center p-4 backdrop-blur-sm"
          onClick={() => setFullscreenImage(null)}
        >
          <div className="relative max-w-6xl max-h-[90vh] w-full flex flex-col items-center">
            <button 
              onClick={() => setFullscreenImage(null)}
              className="absolute -top-10 right-0 text-white hover:text-red-400 bg-slate-800 p-2 rounded-full transition-colors"
            >
              <XCircle size={24} />
            </button>
            <img 
              src={`data:image/${fullscreenImage.startsWith('/9j/') ? 'jpeg' : 'png'};base64,${fullscreenImage}`} 
              alt="Screenshot Expandido" 
              className="object-contain max-h-[85vh] rounded-lg shadow-2xl border border-slate-700"
              onClick={(e) => e.stopPropagation()} 
            />
          </div>
        </div>
      )}

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
          <div className="flex justify-center items-center flex-1 h-64"><Activity className="text-blue-500 animate-spin" size={32} /></div>
        ) : testResults.length > 0 ? (
          <div className="overflow-x-auto flex-1">
            <table className="min-w-full divide-y divide-slate-100">
              <thead className="bg-white">
                <tr>
                  <th className="w-8 px-4 py-3"></th>
                  <th className="px-4 py-3 text-left text-[10px] font-bold text-slate-400 uppercase tracking-wider">Nome do Teste</th>
                  <th className="px-4 py-3 text-center text-[10px] font-bold text-slate-400 uppercase tracking-wider w-24">Evidência</th>
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
                  
                  // FIX: Captura Base64 independentemente do nome do campo no DTO (screenshot vs screenshotBase64)
                  const base64Data = test.screenshotBase64 || test.screenshot;
                  const hasScreenshot = !!base64Data;

                  return (
                    <React.Fragment key={test.id}>
                      <tr onClick={() => toggleRow(test.id, canExpand)} className={`transition-colors group ${canExpand ? 'hover:bg-slate-50/60 cursor-pointer' : 'cursor-default'} ${expandedRowId === test.id ? 'bg-blue-50/20' : ''}`}>
                        <td className="px-4 py-2.5 text-slate-300">
                          {canExpand && <span className="group-hover:text-blue-400 transition-colors">{expandedRowId === test.id ? <ChevronDown size={16} /> : <ChevronRight size={16} />}</span>}
                        </td>
                        <td className={`px-4 py-2.5 text-xs font-mono text-slate-600 break-all transition-colors ${canExpand ? 'group-hover:text-blue-700' : ''}`} title={fullName}>{normalizeTestName(fullName)}</td>
                        
                        <td className="px-4 py-2.5 text-center">
                          {hasScreenshot ? (
                            <ImageIcon size={14} className="text-blue-400 mx-auto" title="Captura de Ecrã disponível" />
                          ) : (
                            <span className="text-[10px] text-slate-300">-</span>
                          )}
                        </td>

                        <td className="px-4 py-2.5 text-center">{test.isFlaky && <AlertTriangle size={14} className="text-amber-500 mx-auto" />}</td>
                        <td className="px-6 py-2.5 text-right">
                          <span className={`inline-flex items-center justify-center gap-1 w-20 py-1 rounded-md text-[10px] font-bold uppercase tracking-widest ${isPass ? 'bg-green-100 text-green-700' : isFail ? 'bg-red-100 text-red-600' : 'bg-slate-100 text-slate-600'}`}>
                            {isPass && <CheckCircle2 size={10} />}
                            {isFail && <XCircle size={10} />}
                            {test.result}
                          </span>
                        </td>
                      </tr>
                      {expandedRowId === test.id && canExpand && (
                        <tr className="bg-slate-50/50 border-b border-slate-100 shadow-inner">
                          <td colSpan="5" className="px-8 py-6">
                            
                            <div className="flex flex-col lg:flex-row gap-6">
                              {/* COLUNA ESQUERDA: Stack Trace / Razão */}
                              <div className="flex-1 flex flex-col gap-3 min-w-0">
                                {test.isFlaky && (
                                  <div className="border-l-2 border-amber-400 bg-amber-50/50 px-3 py-2 rounded-r-md flex items-start gap-2">
                                    <AlertTriangle size={14} className="text-amber-500 mt-0.5 shrink-0" />
                                    <div>
                                      <span className="text-amber-800 font-bold text-[11px] uppercase tracking-wider block mb-0.5">Alerta de Instabilidade</span>
                                      <p className="text-[11px] text-amber-700 leading-relaxed">{test.flakyReason || "Este teste demonstrou instabilidade no histórico recente desta pipeline."}</p>
                                    </div>
                                  </div>
                                )}
                                
                                {isFail && (
                                  <div className="flex flex-col bg-red-50/50 rounded-lg border border-red-100 h-full">
                                    <div className="flex items-center gap-2 px-4 py-3 border-b border-red-100/50 bg-red-100/30 rounded-t-lg">
                                      <XCircle className="text-red-500 shrink-0" size={16} />
                                      <h4 className="font-bold text-red-800 text-xs uppercase tracking-wider">Log de Erro</h4>
                                    </div>
                                    <div className="p-4 overflow-x-auto custom-scrollbar">
                                      {(test.stackTrace || test.errorMessage || test.error_message) ? (
                                        <pre className="text-[11px] text-red-800 font-mono whitespace-pre-wrap leading-relaxed">
                                          {test.stackTrace || test.errorMessage || test.error_message}
                                        </pre>
                                      ) : (
                                        <p className="text-[11px] text-red-600 italic">O teste falhou, mas a infraestrutura não forneceu o motivo.</p>
                                      )}
                                    </div>
                                  </div>
                                )}
                              </div>

                              {/* COLUNA DIREITA: Screenshot Base64 Dinâmico */}
                              {hasScreenshot && (
                                <div className="w-full lg:w-1/3 xl:w-1/4 shrink-0 flex flex-col">
                                  <div className="flex items-center gap-2 px-4 py-3 border border-b-0 border-blue-100 bg-blue-50/80 rounded-t-lg">
                                    <ImageIcon className="text-blue-500 shrink-0" size={16} />
                                    <h4 className="font-bold text-blue-800 text-xs uppercase tracking-wider">Evidência Visual</h4>
                                  </div>
                                  <div className="p-2 border border-blue-100 bg-white rounded-b-lg flex flex-col items-center justify-center">
                                    <div 
                                      className="relative w-full aspect-video bg-slate-100 rounded border border-slate-200 overflow-hidden cursor-zoom-in group"
                                      onClick={() => setFullscreenImage(base64Data)}
                                    >
                                      <img 
                                        src={`data:image/${base64Data.startsWith('/9j/') ? 'jpeg' : 'png'};base64,${base64Data}`} 
                                        alt="Thumbnail do Erro" 
                                        className="w-full h-full object-cover opacity-90 group-hover:opacity-100 transition-opacity"
                                      />
                                      <div className="absolute inset-0 bg-blue-900/0 group-hover:bg-blue-900/10 transition-colors flex items-center justify-center">
                                        <span className="bg-slate-900/80 text-white text-[10px] font-bold px-2 py-1 rounded opacity-0 group-hover:opacity-100 transition-opacity backdrop-blur-sm">
                                          Clique para Expandir
                                        </span>
                                      </div>
                                    </div>
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