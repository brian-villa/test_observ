import { useNavigate, Link } from 'react-router-dom';
import { useForm } from 'react-hook-form';
import { yupResolver } from '@hookform/resolvers/yup';
import * as yup from 'yup';
import api from '../services/api';
import { Lock, Mail, Activity, AlertCircle, User } from 'lucide-react';
import toast from 'react-hot-toast';

const registerSchema = yup.object({
  name: yup.string()
    .min(2, 'O nome deve ter no mínimo 2 caracteres.')
    .required('O nome é obrigatório.'),
  email: yup.string()
    .email('Formato de e-mail inválido.')
    .required('O e-mail é obrigatório.'),
  password: yup.string()
    .min(8, 'A palavra-passe deve ter no mínimo 8 caracteres.')
    .required('A palavra-passe é obrigatória.'),
  confirmPassword: yup.string()
    .oneOf([yup.ref('password')], 'As palavras-passe não coincidem.')
    .required('A confirmação é obrigatória.')
}).required();

export default function Register() {
  const navigate = useNavigate();

  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting }
  } = useForm({
    resolver: yupResolver(registerSchema)
  });

  const onSubmit = async (data) => {
    try {
      await api.post('/auth/register', { 
        name: data.name,
        email: data.email, 
        password: data.password 
      });
      toast.success('Conta criada com sucesso! Pode iniciar sessão.');
      navigate('/login');
    } catch (error) {
      const message = error.response?.data?.message || error.response?.data || 'Erro ao criar conta. Verifique os dados.';
      toast.error(message);
    }
  };

  return (
    <div className="min-h-screen flex items-center justify-center bg-slate-50 px-4 py-12 sm:px-6 lg:px-8">
      <div className="max-w-md w-full bg-white rounded-xl shadow-sm border border-slate-200 p-8">
        
        <div className="sm:mx-auto sm:w-full sm:max-w-sm mb-8">
          <div className="mx-auto w-12 h-12 bg-primary-600 rounded-lg flex items-center justify-center shadow-sm">
            <Activity className="text-white" size={24} />
          </div>
          <h2 className="mt-4 text-center text-2xl font-bold leading-9 tracking-tight text-slate-900">
            Criar conta SGMTA
          </h2>
        </div>

        <form onSubmit={handleSubmit(onSubmit)} className="space-y-6">

           {/* --- CAMPO NOME --- */}
          <div>
            <label className="block text-sm font-medium leading-6 text-slate-900">Nome Completo</label>
            <div className="relative mt-2">
              <div className="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none">
                <User className={`h-5 w-5 ${errors.name ? 'text-red-400' : 'text-slate-400'}`} />
              </div>
              <input
                type="text"
                {...register('name')}
                className={`block w-full rounded-md border-0 py-1.5 pl-10 text-slate-900 shadow-sm ring-1 ring-inset ${errors.name ? 'ring-red-300 focus:ring-2 focus:ring-inset focus:ring-red-500' : 'ring-slate-300 focus:ring-2 focus:ring-inset focus:ring-primary-600'} sm:text-sm sm:leading-6 transition-all`}
                placeholder="João Engenheiro"
              />
            </div>
            {errors.name && <p className="mt-2 text-sm text-red-500 flex items-center gap-1"><AlertCircle size={14} /> {errors.name.message}</p>}
          </div> 
          
          <div>
            <label className="block text-sm font-medium leading-6 text-slate-900">E-mail Profissional</label>
            <div className="relative mt-2">
              <div className="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none">
                <Mail className={`h-5 w-5 ${errors.email ? 'text-red-400' : 'text-slate-400'}`} />
              </div>
              <input
                type="email"
                {...register('email')}
                className={`block w-full rounded-md border-0 py-1.5 pl-10 text-slate-900 shadow-sm ring-1 ring-inset ${errors.email ? 'ring-red-300 focus:ring-2 focus:ring-inset focus:ring-red-500' : 'ring-slate-300 focus:ring-2 focus:ring-inset focus:ring-primary-600'} sm:text-sm sm:leading-6 transition-all`}
                placeholder="engenheiro@empresa.com"
              />
            </div>
            {errors.email && <p className="mt-2 text-sm text-red-500 flex items-center gap-1"><AlertCircle size={14} /> {errors.email.message}</p>}
          </div>

          <div>
            <label className="block text-sm font-medium leading-6 text-slate-900">Palavra-passe</label>
            <div className="relative mt-2">
              <div className="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none">
                <Lock className={`h-5 w-5 ${errors.password ? 'text-red-400' : 'text-slate-400'}`} />
              </div>
              <input
                type="password"
                {...register('password')}
                className={`block w-full rounded-md border-0 py-1.5 pl-10 text-slate-900 shadow-sm ring-1 ring-inset ${errors.password ? 'ring-red-300 focus:ring-2 focus:ring-inset focus:ring-red-500' : 'ring-slate-300 focus:ring-2 focus:ring-inset focus:ring-primary-600'} sm:text-sm sm:leading-6 transition-all`}
                placeholder="Mínimo de 8 caracteres"
              />
            </div>
            {errors.password && <p className="mt-2 text-sm text-red-500 flex items-center gap-1"><AlertCircle size={14} /> {errors.password.message}</p>}
          </div>

          <div>
            <label className="block text-sm font-medium leading-6 text-slate-900">Confirmar Palavra-passe</label>
            <div className="relative mt-2">
              <div className="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none">
                <Lock className={`h-5 w-5 ${errors.confirmPassword ? 'text-red-400' : 'text-slate-400'}`} />
              </div>
              <input
                type="password"
                {...register('confirmPassword')}
                className={`block w-full rounded-md border-0 py-1.5 pl-10 text-slate-900 shadow-sm ring-1 ring-inset ${errors.confirmPassword ? 'ring-red-300 focus:ring-2 focus:ring-inset focus:ring-red-500' : 'ring-slate-300 focus:ring-2 focus:ring-inset focus:ring-primary-600'} sm:text-sm sm:leading-6 transition-all`}
                placeholder="Repita a palavra-passe"
              />
            </div>
            {errors.confirmPassword && <p className="mt-2 text-sm text-red-500 flex items-center gap-1"><AlertCircle size={14} /> {errors.confirmPassword.message}</p>}
          </div>

          <button
            type="submit"
            disabled={isSubmitting}
            className="flex w-full justify-center rounded-md bg-primary-600 px-3 py-2 text-sm font-semibold leading-6 text-white shadow-sm hover:bg-primary-500 focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-primary-600 disabled:opacity-50 transition-colors"
          >
            {isSubmitting ? 'A Registar...' : 'Criar Conta'}
          </button>
        </form>

        <p className="mt-8 text-center text-sm text-slate-500">
          Já possui uma conta?{' '}
          <Link transform="none" to="/login" className="font-semibold leading-6 text-primary-600 hover:text-primary-500">
            Iniciar Sessão
          </Link>
        </p>

      </div>
    </div>
  );
}