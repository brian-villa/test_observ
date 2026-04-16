import { useState, useEffect } from 'react';
import { useForm } from 'react-hook-form';
import { yupResolver } from '@hookform/resolvers/yup';
import * as yup from 'yup';
import api from '../services/api';
import Layout from '../components/Layout';
import ProjectCard from '../components/ProjectCard';
import { Plus, FolderGit2, X, Loader2, RefreshCw, AlertTriangle } from 'lucide-react';
import toast from 'react-hot-toast';

const projectSchema = yup.object({
  name: yup.string().required('O nome do projeto é obrigatório.'),
  description: yup.string().max(500, 'Máximo 500 caracteres.'),
  flakyThreshold: yup.number()
    .typeError('Deve ser um número')
    .min(1, 'Mínimo de 1 falha')
    .max(10, 'Máximo de 10 falhas')
    .required('Obrigatório')
}).required();

export default function Dashboard() {
  const [projects, setProjects] = useState([]);
  const [isLoading, setIsLoading] = useState(true);
  
  //  está a criar ou editar
  const [modalConfig, setModalConfig] = useState({
    isOpen: false,
    mode: 'create', 
    projectData: null
  });

  const { register, handleSubmit, reset, formState: { errors, isSubmitting } } = useForm({
    resolver: yupResolver(projectSchema),
    defaultValues: { flakyThreshold: 3 }
  });

  const fetchProjects = async () => {
    try {
      const response = await api.get('/projects');
      setProjects(response.data);
    } catch (error) {
      toast.error('Erro ao carregar os projetos.');
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => { 
    fetchProjects(); 
  }, []);

  const openCreateModal = () => {
    reset({ name: '', description: '', flakyThreshold: 3 });
    setModalConfig({ isOpen: true, mode: 'create', projectData: null });
  };

  const openEditModal = (project) => {
    reset({ 
      name: project.name, 
      description: project.description || '', 
      flakyThreshold: project.flakyThreshold || 3 
    });
    setModalConfig({ isOpen: true, mode: 'edit', projectData: project });
  };

  const closeModal = () => {
    setModalConfig({ isOpen: false, mode: 'create', projectData: null });
    reset();
  };

  const onSubmit = async (data) => {
    try {
      if (modalConfig.mode === 'create') {
        await api.post('/projects', data);
        toast.success('Projeto criado com sucesso!');
      } else {
        await api.patch(`/projects/${modalConfig.projectData.id}`, data);
        toast.success('Projeto atualizado com sucesso!');
      }
      closeModal();
      fetchProjects();
    } catch (error) {
      toast.error(error.response?.data?.message || 'Erro ao guardar projeto.');
    }
  };

  const handleRegenerateToken = async () => {
    const isConfirmed = window.confirm(
      "ATENÇÃO: Revogar esta API Key fará com que todas as pipelines de CI/CD atuais deixem de funcionar até que a nova chave seja configurada. Deseja continuar?"
    );
    
    if (isConfirmed) {
      try {
        const toastId = toast.loading('A gerar nova chave...');
        await api.patch(`/projects/${modalConfig.projectData.id}/rotate-token`);
        toast.success('API Key revogada e gerada com sucesso!', { id: toastId });
        closeModal();
        fetchProjects();
      } catch (error) {
        toast.error('Erro ao regenerar a API Key.');
      }
    }
  };

  return (
    <Layout>
      <div className="sm:flex sm:items-center sm:justify-between mb-8">
        <div>
          <h1 className="text-2xl font-bold leading-7 text-slate-900 sm:truncate sm:tracking-tight">Meus Projetos</h1>
          <p className="mt-1 text-sm text-slate-500">Selecione um projeto para aceder ao dashboard de métricas e equipa.</p>
        </div>
        <div className="mt-4 sm:ml-16 sm:mt-0 sm:flex-none">
          <button onClick={openCreateModal} className="flex items-center gap-2 rounded-md bg-primary-600 px-3 py-2 text-center text-sm font-semibold text-white shadow-sm hover:bg-primary-500 transition-colors">
            <Plus size={16} /> Novo Projeto
          </button>
        </div>
      </div>

      {isLoading ? (
        <div className="flex justify-center items-center h-64">
          <Loader2 className="animate-spin text-primary-600" size={32} />
        </div>
      ) : projects.length === 0 ? (
        <div className="text-center bg-white border border-dashed border-slate-300 rounded-lg py-16 px-6 shadow-sm">
          <FolderGit2 className="mx-auto h-12 w-12 text-slate-300" />
          <h3 className="mt-2 text-sm font-semibold text-slate-900">Nenhum projeto encontrado</h3>
          <p className="mt-1 text-sm text-slate-500">Comece por criar um novo projeto para gerar uma API Key.</p>
          <div className="mt-6">
            <button onClick={openCreateModal} className="inline-flex items-center gap-2 rounded-md bg-white px-3 py-2 text-sm font-semibold text-primary-600 shadow-sm ring-1 ring-inset ring-slate-300 hover:bg-slate-50 transition-colors">
              <Plus size={16} /> Criar Projeto
            </button>
          </div>
        </div>
      ) : (
        <div className="grid grid-cols-1 gap-6 sm:grid-cols-2 lg:grid-cols-3">
          {projects.map((project) => (
            <ProjectCard 
              key={project.id} 
              project={project} 
              onEdit={openEditModal}
            />
          ))}
        </div>
      )}

      {modalConfig.isOpen && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-900/50 backdrop-blur-sm">
          <div className="bg-white rounded-xl shadow-xl w-full max-w-md overflow-hidden animate-in fade-in zoom-in-95 duration-200">
            <div className="flex justify-between items-center p-6 border-b border-slate-100">
              <h2 className="text-lg font-semibold text-slate-900">
                {modalConfig.mode === 'create' ? 'Novo Projeto' : 'Configurações do Projeto'}
              </h2>
              <button onClick={closeModal} className="text-slate-400 hover:text-slate-600"><X size={20} /></button>
            </div>
            
            <form onSubmit={handleSubmit(onSubmit)} className="p-6 space-y-4">
              <div>
                <label className="block text-sm font-medium text-slate-700 mb-1">Nome do Projeto</label>
                <input type="text" {...register('name')} className={`block w-full rounded-md border-0 py-2 px-3 text-slate-900 shadow-sm ring-1 ring-inset ${errors.name ? 'ring-red-300 focus:ring-red-500' : 'ring-slate-300 focus:ring-primary-600'} sm:text-sm`} />
                {errors.name && <p className="mt-1 text-xs text-red-500">{errors.name.message}</p>}
              </div>
              
              <div>
                <label className="block text-sm font-medium text-slate-700 mb-1">Descrição (Opcional)</label>
                <textarea {...register('description')} rows={2} className="block w-full rounded-md border-0 py-2 px-3 text-slate-900 shadow-sm ring-1 ring-inset ring-slate-300 focus:ring-primary-600 sm:text-sm resize-none" placeholder="Breve descrição da finalidade..." />
              </div>
              
              <div>
                <label className="block text-sm font-medium text-slate-700 mb-1">Limiar de Flakiness (Nº de falhas)</label>
                <input type="number" {...register('flakyThreshold')} className={`block w-full rounded-md border-0 py-2 px-3 text-slate-900 shadow-sm ring-1 ring-inset ${errors.flakyThreshold ? 'ring-red-300 focus:ring-red-500' : 'ring-slate-300 focus:ring-primary-600'} sm:text-sm`} />
                <p className="mt-1 text-[11px] text-slate-500">O teste será marcado como Flaky após este número de falhas seguidas por um sucesso.</p>
                {errors.flakyThreshold && <p className="mt-1 text-xs text-red-500">{errors.flakyThreshold.message}</p>}
              </div>

              {/* Apenas visível em modo Edição */}
              {modalConfig.mode === 'edit' && (
                <div className="mt-6 pt-4 border-t border-red-100 bg-red-50/50 p-4 rounded-lg">
                  <div className="flex items-start gap-3">
                    <AlertTriangle className="text-red-500 shrink-0 mt-0.5" size={18} />
                    <div>
                      <h4 className="text-sm font-semibold text-red-800">Atenção</h4>
                      <p className="text-xs text-red-600 mt-1 mb-3 leading-relaxed">
                        Se a sua API Key foi comprometida, pode revogá-la. Esta ação é irreversível e bloqueará as integrações ativas.
                      </p>
                      <button 
                        type="button" 
                        onClick={handleRegenerateToken}
                        className="flex items-center gap-1.5 px-3 py-1.5 text-xs font-semibold text-red-700 bg-red-100 hover:bg-red-200 rounded-md transition-colors"
                      >
                        <RefreshCw size={12} /> Gerar Nova API Key
                      </button>
                    </div>
                  </div>
                </div>
              )}

              <div className="mt-6 flex justify-end gap-3 pt-4 border-t border-slate-100">
                <button type="button" onClick={closeModal} className="px-4 py-2 text-sm font-medium text-slate-700 bg-white border border-slate-300 rounded-md hover:bg-slate-50">Cancelar</button>
                <button type="submit" disabled={isSubmitting} className="px-4 py-2 text-sm font-medium text-white bg-primary-600 rounded-md hover:bg-primary-500 disabled:opacity-50">
                  {isSubmitting ? 'A Guardar...' : (modalConfig.mode === 'create' ? 'Criar Projeto' : 'Guardar Alterações')}
                </button>
              </div>
            </form>

          </div>
        </div>
      )}
    </Layout>
  );
}