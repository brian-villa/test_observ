import { useState, useEffect, useCallback, useMemo, useRef } from 'react';
import { useParams, Link, useNavigate } from 'react-router-dom';
import Layout from '../components/Layout';
import { ArrowLeft, AlertTriangle, Clock, TrendingUp, BarChart2, RefreshCw, Search, Loader2, GitBranch, Layers, List, PlayCircle, ChevronRight, GitMerge, ChevronDown } from 'lucide-react';
import api from '../services/api';
import toast from 'react-hot-toast';
import { XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer, AreaChart, Area, BarChart, Bar } from 'recharts';

function SkeletonCard() { return (<div className="bg-white rounded-xl border border-slate-200 p-6 animate-pulse"><div className="h-3 w-24 bg-slate-200 rounded mb-4" /><div className="h-10 w-16 bg-slate-200 rounded" /></div>); }

function MetricCard({ label, value, accent, sub }) {
  const accentMap = { green: 'border-t-green-500', blue: 'border-t-blue-500', red: 'border-t-red-500', amber: 'border-t-amber-500', slate: 'border-t-slate-400' };
  const textMap   = { green: 'text-slate-900', blue: 'text-slate-900', red: 'text-red-700', amber: 'text-amber-700', slate: 'text-slate-900' };
  const labelMap  = { green: 'text-slate-500', blue: 'text-slate-500', red: 'text-red-600', amber: 'text-amber-600', slate: 'text-slate-500' };
  return (
    <div className={`bg-white p-6 rounded-xl border border-slate-200 shadow-sm border-t-4 ${accentMap[accent] || accentMap.slate}`}>
      <p className={`text-[11px] font-bold uppercase tracking-widest ${labelMap[accent] || labelMap.slate}`}>{label}</p>
      <div className="flex items-end gap-2 mt-3"><span className={`text-4xl font-black tabular-nums ${textMap[accent] || textMap.slate}`}>{value}</span></div>
      {sub && <p className="text-xs text-slate-400 mt-1">{sub}</p>}
    </div>
  );
}

function CustomTooltip({ active, payload, label, mode }) {
  if (!active || !payload?.length) return null;
  const dataObj = payload[0].payload;
  const flakyCount = dataObj.flakyCount || 0;

  return (
    <div style={{ background: 'white', border: '1px solid #e2e8f0', borderRadius: '8px', padding: '8px 12px', fontSize: '12px', boxShadow: '0 4px 12px rgba(0,0,0,0.08)', minWidth: '120px', zIndex: 100 }}>
      <p style={{ color: '#64748b', marginBottom: '6px', fontWeight: 600, borderBottom: '1px solid #f1f5f9', paddingBottom: '4px' }}>{label}</p>
      {payload.map(p => (
        <p key={p.dataKey || p.name} style={{ color: p.color || p.payload?.fill, margin: '2px 0' }}>
          {p.name}: <span style={{ fontWeight: 700 }}>{mode === 'duration' ? p.value + 's' : Math.round(p.value)}</span>
        </p>
      ))}
      {mode !== 'duration' && (
        <p style={{ color: '#f59e0b', margin: '2px 0 0 0' }}>Flaky: <span style={{ fontWeight: 700 }}>{flakyCount}</span></p>
      )}
    </div>
  );
}

function useOutsideClick(ref, callback) {
  useEffect(() => {
    function handleClickOutside(event) { if (ref.current && !ref.current.contains(event.target)) callback(); }
    document.addEventListener("mousedown", handleClickOutside);
    return () => document.removeEventListener("mousedown", handleClickOutside);
  }, [ref, callback]);
}

export default function ProjectDetails() {
  const { id } = useParams();
  const navigate = useNavigate();

  const [isLoading, setIsLoading] = useState(true);
  const [isRefreshing, setIsRefreshing] = useState(false);
  const [chartMode, setChartMode] = useState('results'); 
  const [chartTab, setChartTab] = useState('versions');

  const [availableVersions, setAvailableVersions] = useState([]);
  const [selectedVersion, setSelectedVersion] = useState('');
  const [availableBranches, setAvailableBranches] = useState([]);
  const [selectedBranch, setSelectedBranch] = useState('');
  const [availableSuites, setAvailableSuites] = useState([]);
  const [selectedSuite, setSelectedSuite] = useState('');

  const [searchQuery, setSearchQuery] = useState('');
  const [searchResults, setSearchResults] = useState([]);
  const [isSearching, setIsSearching] = useState(false);
  const [showDropdown, setShowDropdown] = useState(false);
  const searchRef = useRef(null);
  useOutsideClick(searchRef, () => setShowDropdown(false));

  const [projectInfo, setProjectInfo] = useState({ 
    projectName: 'A carregar...', 
    lastExecutionTime: '',
    healthScore: 100,
    totalFlaky: 0
  });
  const [rawHistoryData, setRawHistoryData] = useState([]);

  useEffect(() => {
    if (!selectedVersion) setChartTab('versions');
    else setChartTab('builds');
  }, [selectedVersion]);

  useEffect(() => {
    if (id) {
      api.get(`/api/v1/projects/${id}/dashboard/filters`).then(res => {
        setAvailableVersions(res.data.versions || []);
      }).catch(err => console.error(err));
    }
  }, [id]);

  useEffect(() => {
    if (id && selectedVersion) {
      setSelectedBranch(''); setSelectedSuite('');
      api.get(`/api/v1/projects/${id}/dashboard/filters/version?versionName=${encodeURIComponent(selectedVersion)}`).then(res => {
        setAvailableBranches(res.data.branches || []);
        setAvailableSuites(res.data.suites || []);
      }).catch(err => console.error(err));
    }
  }, [id, selectedVersion]);

  useEffect(() => {
    const timer = setTimeout(() => {
      if (searchQuery.trim().length >= 3) {
        setIsSearching(true);
        const params = new URLSearchParams({ query: searchQuery });
        if (selectedBranch) params.append('branchName', selectedBranch);
        if (selectedVersion) params.append('versionName', selectedVersion);
        if (selectedSuite) params.append('suiteName', selectedSuite);
        
        api.get(`/api/v1/projects/${id}/dashboard/search?${params.toString()}`)
          .then(res => { setSearchResults(res.data || []); setShowDropdown(true); })
          .finally(() => setIsSearching(false));
      } else { setSearchResults([]); setShowDropdown(false); }
    }, 500);
    return () => clearTimeout(timer);
  }, [searchQuery, id, selectedBranch, selectedVersion, selectedSuite]);

  const fetchDashboardData = useCallback(async (showRefreshSpinner = false) => {
    try {
      if (showRefreshSpinner) setIsRefreshing(true); else setIsLoading(true);
      const params = new URLSearchParams({ size: 300 }); 
      if (selectedBranch) params.append('branchName', selectedBranch);
      if (selectedVersion) params.append('versionName', selectedVersion);
      if (selectedSuite) params.append('suiteName', selectedSuite);

      const historyRes = await api.get(`/api/v1/projects/${id}/dashboard/history?${params.toString()}`);
      const content = historyRes.data.content || [];
      setRawHistoryData([...content].reverse());

      try {
        const metricsParams = new URLSearchParams();
        if (selectedBranch) metricsParams.append('branchName', selectedBranch);
        if (selectedVersion) metricsParams.append('versionName', selectedVersion);
        if (selectedSuite) metricsParams.append('suiteName', selectedSuite);

        const metricsRes = await api.get(`/api/v1/projects/${id}/dashboard/metrics/global?${metricsParams.toString()}`);
        setProjectInfo({ 
            projectName: metricsRes.data.projectName, 
            lastExecutionTime: metricsRes.data.lastExecutionTime,
            healthScore: metricsRes.data.healthScore,
            totalFlaky: metricsRes.data.totalFlaky 
        });
      } catch (e) {
        setProjectInfo(prev => ({ ...prev, projectName: 'Detalhes do Projeto', lastExecutionTime: 'Sem dados' }));
      }
    } catch (error) { toast.error('Erro ao sincronizar dados.'); } 
    finally { setIsLoading(false); setIsRefreshing(false); }
  }, [id, selectedBranch, selectedVersion, selectedSuite]);

  useEffect(() => { if (id) fetchDashboardData(); }, [fetchDashboardData, id, selectedVersion]);

  // AGRUPAÇÃO VERSÕES (Modo Global)
  const groupedVersions = useMemo(() => {
    const map = new Map();
    rawHistoryData.forEach(run => {
      const vName = run.versionName && run.versionName !== 'N/A' ? run.versionName : 'Sem Versão';
      if (!map.has(vName)) map.set(vName, { name: vName, passedCount: 0, failedCount: 0, flakyCount: 0, builds: new Set(), startTime: run.startTime });
      const v = map.get(vName);
      v.passedCount += (run.passedCount || 0);
      v.failedCount += (run.failedCount || 0);
      v.flakyCount += (run.flakyCount || 0);
      v.builds.add(run.runId || run.id); // Conta Builds unicas com o runId
      if (new Date(run.startTime) > new Date(v.startTime)) v.startTime = run.startTime;
    });
    return Array.from(map.values()).map(v => ({ ...v, buildCount: v.builds.size })).sort((a,b) => new Date(a.startTime) - new Date(b.startTime));
  }, [rawHistoryData]);

  // AGRUPAÇÃO BUILDS: A Mágica de Agrupar pelo runId!
  const groupedBuilds = useMemo(() => {
    const map = new Map();
    rawHistoryData.forEach(run => {
      // 1. Usa o runId da Pipeline! Isto impede que Retries ou builds com o mesmo nome se juntem!
      const key = run.runId || run.id; 

      if (!map.has(key)) map.set(key, { 
        runId: key,
        buildName: run.buildName || `Build ${key.substring(0,8)}`, 
        startTime: run.startTime, 
        passedCount: 0, 
        failedCount: 0, 
        flakyCount: 0, 
        durationMillis: 0, 
        suites: new Set(), 
        executions: [] 
      });
      const b = map.get(key);
      b.passedCount += (run.passedCount || 0);
      b.failedCount += (run.failedCount || 0);
      b.flakyCount += (run.flakyCount || 0); 
      b.durationMillis += (run.durationMillis || 0);
      if (run.suiteName) b.suites.add(run.suiteName.replace('Backend-', ''));
      b.executions.push(run);
      if (new Date(run.startTime) < new Date(b.startTime)) b.startTime = run.startTime;
    });
    return Array.from(map.values()).map(b => ({ ...b, suites: Array.from(b.suites) }));
  }, [rawHistoryData]);

  const dynamicMetrics = useMemo(() => {
    let totalPass = 0; let totalFail = 0;
    if (!selectedVersion) {
        groupedVersions.forEach(v => { totalPass += v.passedCount; totalFail += v.failedCount; });
    } else {
        groupedBuilds.forEach(b => { totalPass += b.passedCount; totalFail += b.failedCount; });
    }
    const total = totalPass + totalFail;
    
    // Agora contas também de forma segura pelo runId!
    const uniqueBuildsCount = !selectedVersion ? Array.from(new Set(rawHistoryData.map(r => r.runId || r.id))).length : groupedBuilds.length;

    return { total, buildsCount: uniqueBuildsCount };
  }, [groupedBuilds, groupedVersions, rawHistoryData, selectedVersion]);

  const healthStatus = useMemo(() => {
    const s = projectInfo.healthScore;
    if (s >= 90) return { accent: 'green', label: 'Estável' };
    if (s >= 70) return { accent: 'amber', label: 'Atenção recomendada' };
    return { accent: 'red', label: 'Requer ação imediata' };
  }, [projectInfo.healthScore]);

  const buildChartData = useMemo(() => groupedBuilds.map(b => ({ 
    name: b.buildName, 
    passedCount: b.passedCount, 
    failedCount: b.failedCount, 
    flakyCount: b.flakyCount,
    duration: Number((b.durationMillis / 1000).toFixed(0)), 
    fullBuildObj: b 
  })), [groupedBuilds]);

  const handleChartClick = useCallback((state) => {
    if (!state?.activeLabel) return;
    
    if (!selectedVersion && chartTab === 'versions') {
        setSelectedVersion(state.activeLabel);
        setChartTab('builds');
    } else {
        const hit = buildChartData.find(item => item.name === state.activeLabel);
        if (hit && hit.fullBuildObj) {
            navigate(`/projects/${id}/executions/${hit.fullBuildObj.executions[0].id || hit.fullBuildObj.executions[0].executionId}`, {
                state: { executionIds: hit.fullBuildObj.executions.map(e => e.id || e.executionId), buildName: hit.fullBuildObj.buildName }
            });
        }
    }
  }, [selectedVersion, chartTab, buildChartData, id, navigate]);

  return (
    <Layout>
      <div className="mb-6 flex flex-col xl:flex-row xl:items-end justify-between gap-4 border-b border-slate-200 pb-6">
        <div className="flex-1">
          <Link to="/dashboard" className="inline-flex items-center text-xs font-medium text-slate-400 mb-3 hover:text-slate-600 transition-colors gap-1">
            <ArrowLeft size={14} /> Voltar ao Gestor
          </Link>
          <div className="flex flex-wrap items-center gap-2 mb-1.5">
            <h1 className="text-2xl font-bold text-slate-900 leading-tight">{projectInfo.projectName}</h1>
            <span className="text-slate-300 text-xl font-light">/</span>
            <div className="relative flex items-center gap-1.5 bg-white border border-slate-200 rounded-lg px-2.5 py-1.5 shadow-sm hover:border-slate-300 transition-colors group">
              <GitBranch size={12} className="text-slate-400 shrink-0" />
              <select value={selectedVersion} onChange={(e) => setSelectedVersion(e.target.value)} className="bg-transparent text-sm font-semibold text-slate-700 outline-none cursor-pointer appearance-none pr-4 max-w-[180px]">
                <option value="">Todas as Versões</option>
                {availableVersions.map(v => <option key={v} value={v}>{v}</option>)}
              </select>
              <ChevronDown size={12} className="absolute right-2 text-slate-400 pointer-events-none group-hover:text-slate-600 transition-colors" />
            </div>
            <button onClick={() => fetchDashboardData(true)} disabled={isRefreshing} className="p-1.5 rounded-lg text-slate-400 hover:text-blue-500 hover:bg-blue-50 transition-colors" title="Atualizar">
              <RefreshCw size={14} className={isRefreshing ? 'animate-spin' : ''} />
            </button>
          </div>
          <p className="text-xs text-slate-400 flex items-center gap-1.5"><Clock size={12} /> Última execução: {projectInfo.lastExecutionTime}</p>
        </div>

        <div className="flex flex-col md:flex-row items-end gap-3 z-20 w-full xl:w-auto mt-2 xl:mt-0">
          <div className="relative w-full md:w-72" ref={searchRef}>
            <div className="relative flex items-center w-full">
              <Search className="absolute left-3 text-slate-400" size={14} />
              <input type="text" placeholder={selectedVersion ? `Pesquisar na versão ${selectedVersion}…` : "Pesquisar no projeto…"} value={searchQuery} onChange={(e) => setSearchQuery(e.target.value)}
                onFocus={() => { if (searchQuery.length >= 3) setShowDropdown(true); }}
                className="w-full pl-9 pr-8 py-2 text-xs bg-white border border-slate-200 rounded-xl shadow-sm focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-blue-500 placeholder:text-slate-400"
              />
              {isSearching && <Loader2 className="absolute right-3 text-slate-400 animate-spin" size={13} />}
            </div>
            {showDropdown && (
              <div className="absolute top-full left-0 right-0 mt-1.5 bg-white rounded-xl shadow-lg border border-slate-100 max-h-80 overflow-y-auto z-50">
                {searchResults.length > 0 ? (
                  <ul className="py-1.5">
                    {searchResults.map((result) => (
                      <li key={result.id}>
                        <button onClick={() => { setShowDropdown(false); navigate(`/projects/${id}/executions/${result.testExecutionId}`, { state: { search: result.testCaseName, status: 'ALL', flakyOnly: false, buildName: 'Resultado da Pesquisa' } }); }}
                          className="w-full px-4 py-2.5 hover:bg-slate-50 transition-colors flex flex-col items-start gap-1 text-left border-b border-slate-50 last:border-0">
                          <span className="text-xs font-mono text-slate-700 truncate w-full">{result.testCaseName}</span>
                          <div className="flex items-center gap-2">
                            {result.result === 'PASS' ? <span className="text-[10px] font-bold text-green-600">Passou</span> : <span className="text-[10px] font-bold text-red-600">Falhou</span>}
                          </div>
                        </button>
                      </li>
                    ))}
                  </ul>
                ) : <div className="px-4 py-6 text-center text-xs text-slate-400 italic">Nenhum teste encontrado.</div>}
              </div>
            )}
          </div>

          {(availableBranches.length > 0 || availableSuites.length > 0) && (
            <div className="flex items-center bg-white border border-slate-200 rounded-xl p-1 gap-0.5 w-full md:w-auto overflow-x-auto shadow-sm">
              {availableBranches.length > 0 && (
                <div className="flex items-center shrink-0 px-2 gap-1">
                  <GitBranch size={12} className="text-slate-400" />
                  <select value={selectedBranch} onChange={(e) => setSelectedBranch(e.target.value)} className="bg-transparent text-xs font-semibold text-slate-600 outline-none cursor-pointer py-1 w-[100px] truncate">
                    <option value="">Todas Branches</option>
                    {availableBranches.map(b => <option key={b} value={b}>{b}</option>)}
                  </select>
                </div>
              )}
              {availableSuites.length > 0 && (
                <div className="flex items-center shrink-0 px-2 border-l border-slate-200 gap-1">
                  <Layers size={12} className="text-slate-400" />
                  <select value={selectedSuite} onChange={(e) => setSelectedSuite(e.target.value)} className="bg-transparent text-xs font-semibold text-slate-600 outline-none cursor-pointer py-1 w-[100px] truncate">
                    <option value="">Todas Suites</option>
                    {availableSuites.map(s => <option key={s} value={s}>{s}</option>)}
                  </select>
                </div>
              )}
            </div>
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
          {selectedVersion && (
            <div className="grid grid-cols-1 md:grid-cols-3 gap-5 mb-7">
              <MetricCard label={`Saúde Versão ${selectedVersion}`} value={`${projectInfo.healthScore}%`} accent={healthStatus.accent} sub={healthStatus.label} />
              <MetricCard label="Builds Executadas" value={dynamicMetrics.buildsCount} accent="slate" sub={`${dynamicMetrics.total} Testes Processados`} />
              <MetricCard label="Flakys na Versão" value={projectInfo.totalFlaky} accent={projectInfo.totalFlaky > 0 ? 'amber' : 'green'} sub={projectInfo.totalFlaky === 0 ? 'Sem instabilidades' : 'Ação necessária'} />
            </div>
          )}

          <div className="bg-white rounded-xl border border-slate-200 p-5 mb-7 shadow-sm">
            <div className="flex flex-col md:flex-row items-start md:items-center justify-between mb-5 gap-3 border-b border-slate-100 pb-4">
              <div className="flex items-center gap-2">
                {!selectedVersion ? <GitMerge size={14} className="text-indigo-500" /> : <TrendingUp size={14} className="text-blue-500" />}
                <h3 className="font-semibold text-slate-600 text-xs uppercase tracking-wider">
                  {!selectedVersion ? 'Comparação de Versões' : `Evolução de Builds — ${selectedVersion}`}
                </h3>
              </div>
              
              <div className="flex items-center gap-2">
                <button onClick={() => navigate(`/projects/${id}/build-history`, { state: { versionName: selectedVersion, branchName: selectedBranch, suiteName: selectedSuite } })}
                  className="flex items-center gap-1 px-2.5 py-1.5 text-[11px] font-bold text-blue-600 bg-blue-50 border border-blue-100 rounded-md hover:bg-blue-100 uppercase tracking-wider transition-colors">
                  <List size={11} /> Ver Lista Técnica
                </button>
                {selectedVersion && (
                  <div className="flex bg-slate-50 border border-slate-200 rounded-lg p-0.5 gap-0.5 hidden sm:flex">
                    <button onClick={() => setChartMode('duration')} className={`px-2.5 py-1 text-[11px] font-semibold rounded-md transition-all ${chartMode === 'duration' ? 'bg-white shadow text-slate-700' : 'text-slate-400 hover:text-slate-500'}`}>Tempo</button>
                    <button onClick={() => setChartMode('results')} className={`px-2.5 py-1 text-[11px] font-semibold rounded-md transition-all ${chartMode === 'results' ? 'bg-white shadow text-slate-700' : 'text-slate-400 hover:text-slate-500'}`}>Resultados</button>
                  </div>
                )}
              </div>
            </div>

            <div className="h-72">
              {!selectedVersion ? (
                groupedVersions.length > 0 ? (
                  <ResponsiveContainer width="100%" height="100%">
                    <BarChart data={groupedVersions} onClick={handleChartClick} style={{ cursor: 'pointer' }} margin={{ top: 8, right: 16, left: 0, bottom: 8 }} barCategoryGap="28%" barGap={2}>
                      <CartesianGrid horizontal vertical={false} stroke="#f1f5f9" />
                      <XAxis dataKey="name" tick={{ fill: '#94a3b8', fontSize: 11, fontWeight: 600 }} axisLine={false} tickLine={false} dy={8} interval={0} />
                      <YAxis tick={{ fill: '#cbd5e1', fontSize: 10 }} axisLine={false} tickLine={false} width={36} />
                      <Tooltip content={<CustomTooltip mode="results" />} cursor={{ fill: '#f8fafc' }} />
                      <Bar dataKey="passedCount" name="Passou" stackId="a" fill="#86efac" radius={[0, 0, 0, 0]} maxBarSize={80} />
                      <Bar dataKey="failedCount" name="Falhou" stackId="a" fill="#fca5a5" radius={[3, 3, 0, 0]} maxBarSize={80} />
                    </BarChart>
                  </ResponsiveContainer>
                ) : (<div className="flex justify-center items-center h-full"><p className="text-xs text-slate-300 italic">Sem versões para mostrar.</p></div>)
              ) : (
                buildChartData.length > 0 ? (
                  <ResponsiveContainer width="100%" height="100%">
                    {chartMode === 'results' ? (
                      <BarChart data={buildChartData} onClick={handleChartClick} style={{ cursor: 'pointer' }} margin={{ top: 8, right: 16, left: 0, bottom: 8 }} barCategoryGap="30%" barGap={1}>
                        <CartesianGrid horizontal vertical={false} stroke="#f1f5f9" />
                        <XAxis dataKey="name" tick={{ fill: '#94a3b8', fontSize: 10, fontWeight: 500 }} axisLine={false} tickLine={false} dy={8} interval={0} />
                        <YAxis tick={{ fill: '#cbd5e1', fontSize: 10 }} axisLine={false} tickLine={false} width={36} />
                        <Tooltip content={<CustomTooltip mode="results" />} cursor={{ fill: '#f8fafc' }} />
                        <Bar dataKey="passedCount" name="Passou" stackId="a" fill="#86efac" radius={[0, 0, 2, 2]} />
                        <Bar dataKey="failedCount" name="Falhou" stackId="a" fill="#fca5a5" radius={[2, 2, 0, 0]} />
                      </BarChart>
                    ) : (
                      <AreaChart data={buildChartData} onClick={handleChartClick} style={{ cursor: 'pointer' }} margin={{ top: 8, right: 16, left: 0, bottom: 8 }}>
                        <CartesianGrid horizontal vertical={false} stroke="#f1f5f9" />
                        <XAxis dataKey="name" tick={{ fill: '#94a3b8', fontSize: 10, fontWeight: 500 }} axisLine={false} tickLine={false} dy={8} interval={0} />
                        <YAxis tick={{ fill: '#cbd5e1', fontSize: 10 }} axisLine={false} tickLine={false} width={36} unit="s" />
                        <Tooltip content={<CustomTooltip mode="duration" />} cursor={{ stroke: '#e2e8f0', strokeWidth: 1, strokeDasharray: '3 3' }} />
                        <Area type="monotone" dataKey="duration" name="Duração" stroke="#3b82f6" strokeWidth={1.5} fill="#eff6ff" />
                      </AreaChart>
                    )}
                  </ResponsiveContainer>
                ) : (<div className="flex justify-center items-center h-full"><p className="text-xs text-slate-300 italic">Sem builds para esta versão.</p></div>)
              )}
            </div>

            {(!selectedVersion || chartMode === 'results') && (
              <div className="flex items-center gap-5 mt-3 justify-center">
                <span className="flex items-center gap-1.5 text-[10px] font-semibold text-slate-500 uppercase tracking-wide"><span className="w-2.5 h-2.5 rounded-sm bg-green-300 inline-block" /> Passou</span>
                <span className="flex items-center gap-1.5 text-[10px] font-semibold text-slate-500 uppercase tracking-wide"><span className="w-2.5 h-2.5 rounded-sm bg-red-300 inline-block" /> Falhou</span>
                <span className="flex items-center gap-1.5 text-[10px] font-semibold text-slate-500 uppercase tracking-wide"><span className="w-2.5 h-2.5 rounded-sm bg-amber-400 inline-block" /> Flaky</span>
              </div>
            )}
          </div>

          <div className="bg-white rounded-xl border border-slate-200 shadow-sm overflow-hidden">
            <div className="px-6 py-3.5 border-b border-slate-100 bg-slate-50/50 flex justify-between items-center">
              <h3 className="text-[11px] font-bold text-slate-400 uppercase tracking-wider">{!selectedVersion ? 'Comparativo de Versões' : 'Histórico de Builds Agregadas'}</h3>
            </div>
            <div className="divide-y divide-slate-100 max-h-[500px] overflow-y-auto custom-scrollbar">
              {!selectedVersion ? (
                groupedVersions.slice().reverse().map((v) => (
                  <div key={v.name} onClick={() => setSelectedVersion(v.name)} className="group px-5 py-4 hover:bg-slate-50 transition-all cursor-pointer flex flex-col sm:flex-row sm:items-center justify-between gap-3">
                    <div className="flex items-center gap-3.5">
                      <div className="p-2 bg-indigo-50 text-indigo-500 rounded-lg group-hover:bg-indigo-600 group-hover:text-white transition-colors shrink-0"><GitMerge size={16} /></div>
                      <div>
                        <p className="text-sm font-semibold text-slate-800 group-hover:text-indigo-700 transition-colors">Versão {v.name}</p>
                        <div className="flex flex-wrap items-center gap-x-2 gap-y-1.5 mt-1">
                          <span className="text-[10px] font-semibold text-slate-500 bg-slate-100 px-1.5 py-0.5 rounded">{v.buildCount} Builds</span>
                          <span className="text-[10px] font-semibold text-green-700 bg-green-100 px-1.5 py-0.5 rounded">{v.passedCount} Pass</span>
                          <span className={`text-[10px] font-semibold px-1.5 py-0.5 rounded ${v.failedCount > 0 ? 'bg-red-100 text-red-700' : 'bg-slate-100 text-slate-400'}`}>{v.failedCount} Fail</span>
                        </div>
                      </div>
                    </div>
                    <ChevronRight size={16} className="text-slate-300 group-hover:text-indigo-400 transition-colors hidden sm:block" />
                  </div>
                ))
              ) : (
                groupedBuilds.slice().reverse().map((build) => (
                  <div key={build.buildName} onClick={() => navigate(`/projects/${id}/executions/${build.executions[0].id || build.executions[0].executionId}`, { state: { executionIds: build.executions.map(e => e.id || e.executionId), buildName: build.buildName } })} className="group px-5 py-4 hover:bg-slate-50 transition-all cursor-pointer flex flex-col sm:flex-row sm:items-center justify-between gap-3">
                    <div className="flex items-center gap-3.5">
                      <div className="p-2 bg-blue-50 text-blue-500 rounded-lg group-hover:bg-blue-600 group-hover:text-white transition-colors shrink-0"><PlayCircle size={16} /></div>
                      <div>
                        <div className="flex flex-wrap items-center gap-2 mb-1">
                          <p className="text-sm font-semibold text-slate-800 group-hover:text-blue-700 transition-colors">{build.buildName}</p>
                          <div className="flex gap-1">{build.suites.map(s => (<span key={s} className="text-[9px] font-bold uppercase bg-slate-200 text-slate-500 px-1.5 py-0.5 rounded">{s}</span>))}</div>
                        </div>
                        <div className="flex flex-wrap items-center gap-x-3 gap-y-1">
                          <span className="text-[11px] text-slate-400">{new Date(build.startTime).toLocaleString()}</span>
                          <span className="text-[10px] font-semibold text-green-700 bg-green-100 px-1.5 py-0.5 rounded">{build.passedCount} Pass</span>
                          <span className={`text-[10px] font-semibold px-1.5 py-0.5 rounded ${build.failedCount > 0 ? 'bg-red-100 text-red-700' : 'bg-slate-100 text-slate-400'}`}>{build.failedCount} Fail</span>
                          {build.flakyCount > 0 && <span className="text-[10px] font-semibold text-amber-700 bg-amber-100 px-1.5 py-0.5 rounded">{build.flakyCount} Flaky</span>}
                        </div>
                      </div>
                    </div>
                    <ChevronRight size={16} className="text-slate-300 group-hover:text-blue-400 transition-colors hidden sm:block" />
                  </div>
                ))
              )}
            </div>
          </div>
        </>
      )}
    </Layout>
  );
}