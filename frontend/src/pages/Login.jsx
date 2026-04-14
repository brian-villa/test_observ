import { useContext } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { useForm } from 'react-hook-form';
import { yupResolver } from '@hookform/resolvers/yup';
import * as yup from 'yup';
import { AuthContext } from '../contexts/AuthContext';
import { Lock, Mail, Activity, AlertCircle } from 'lucide-react';

const loginSchema = yup.object({
  email: yup.string()
    .email('Formato de e-mail inválido.')
    .required('O e-mail é obrigatório.'),
  password: yup.string()
    .required('A palavra-passe é obrigatória.')
}).required();

export default function Login() {
  const { login } = useContext(AuthContext);
  const navigate = useNavigate();

  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting }
  } = useForm({
    resolver: yupResolver(loginSchema)
  });

  // 3. Submissão
  const onSubmit = async (data) => {
    try {
      await login(data.email, data.password);
      navigate('/dashboard');
    } catch (error) {
      
    }
  };

  return (
    <div className="min-h-screen flex items-center justify-center bg-slate-50 px-4">
      <div className="max-w-md w-full bg-white rounded-lg shadow-sm border border-slate-200 p-8">
        
        <div className="flex flex-col items-center mb-8">
          <div className="w-12 h-12 bg-primary-600 rounded-lg flex items-center justify-center mb-4 shadow-sm">
            <Activity className="text-white" size={24} />
          </div>
          <h2 className="text-2xl font-semibold text-slate-900">SGMTA</h2>
          <p className="text-sm text-slate-500 mt-1">Acesso Seguro à Plataforma</p>
        </div>

        <form onSubmit={handleSubmit(onSubmit)} className="space-y-5">
          
          <div>
            <label className="block text-sm font-medium text-slate-700 mb-1">E-mail Profissional</label>
            <div className="relative">
              <div className="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none">
                <Mail className={`h-5 w-5 ${errors.email ? 'text-red-400' : 'text-slate-400'}`} />
              </div>
              <input
                type="email"
                {...register('email')}
                className={`block w-full pl-10 pr-3 py-2 border rounded-md focus:outline-none focus:ring-2 sm:text-sm transition-colors bg-white ${
                  errors.email 
                    ? 'border-red-300 focus:ring-red-500 focus:border-red-500' 
                    : 'border-slate-300 focus:ring-primary-500 focus:border-primary-500'
                }`}
                placeholder="engenheiro@empresa.com"
              />
            </div>
            {errors.email && (
              <p className="mt-1 text-sm text-red-500 flex items-center gap-1">
                <AlertCircle size={14} /> {errors.email.message}
              </p>
            )}
          </div>

          <div>
            <label className="block text-sm font-medium text-slate-700 mb-1">Palavra-passe</label>
            <div className="relative">
              <div className="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none">
                <Lock className={`h-5 w-5 ${errors.password ? 'text-red-400' : 'text-slate-400'}`} />
              </div>
              <input
                type="password"
                {...register('password')}
                className={`block w-full pl-10 pr-3 py-2 border rounded-md focus:outline-none focus:ring-2 sm:text-sm transition-colors bg-white ${
                  errors.password 
                    ? 'border-red-300 focus:ring-red-500 focus:border-red-500' 
                    : 'border-slate-300 focus:ring-primary-500 focus:border-primary-500'
                }`}
                placeholder="••••••••"
              />
            </div>
            {errors.password && (
              <p className="mt-1 text-sm text-red-500 flex items-center gap-1">
                <AlertCircle size={14} /> {errors.password.message}
              </p>
            )}
          </div>

          <button
            type="submit"
            disabled={isSubmitting}
            className="w-full flex justify-center py-2.5 px-4 border border-transparent rounded-md shadow-sm text-sm font-medium text-white bg-primary-600 hover:bg-primary-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-primary-500 disabled:opacity-50 disabled:cursor-not-allowed transition-colors mt-2"
          >
            {isSubmitting ? 'A Autenticar...' : 'Entrar no Sistema'}
          </button>
        </form>

        {/* --- REGISTO --- */}
        <p className="mt-8 text-center text-sm text-slate-500">
          Não possui uma conta?{' '}
          <Link to="/register" className="font-semibold leading-6 text-primary-600 hover:text-primary-500 transition-colors">
            Registar Acesso
          </Link>
        </p>

      </div>
    </div>
  );
}