import { useParams } from 'react-router-dom';
import Layout from '../components/Layout';

export default function ProjectDetails() {
  const { id } = useParams();

  return (
    <Layout>
      <div className="bg-white rounded-lg shadow-sm border border-slate-200 p-8 text-center mt-12">
        <h2 className="text-xl font-semibold text-slate-900 mb-2">
          Detalhes do Projeto
        </h2>
        <p className="text-slate-500 mb-4">
          teste
        </p>
        <code className="bg-slate-100 text-slate-600 px-3 py-1 rounded text-sm">
            ID do Projeto: {id}
        </code>
      </div>
    </Layout>
  );
}