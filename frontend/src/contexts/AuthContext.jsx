import { createContext, useState, useEffect } from 'react';
import api from '../services/api';
import toast from 'react-hot-toast';

export const AuthContext = createContext({});

export const AuthProvider = ({ children }) => {
  const [token, setToken] = useState(null);
  const [loading, setLoading] = useState(true);

  // Quando a aplicação arranca, verifica se já existe um token guardado
  useEffect(() => {
    const storedToken = localStorage.getItem('@SGMTA:token');
    if (storedToken) {
      setToken(storedToken);
    }
    setLoading(false);
  }, []);

  // Função central de login que será chamada pelo formulário
  const login = async (email, password) => {
    try {
      const response = await api.post('/auth/login', { email, password });
      
      // Dependendo de como o teu backend devolve a resposta (String simples ou objeto JSON)
      const jwtToken = response.data.token || response.data; 
      
      localStorage.setItem('@SGMTA:token', jwtToken);
      setToken(jwtToken);
      toast.success('Sessão iniciada com sucesso.');
    } catch (error) {
      const message = error.response?.data?.message || error.response?.data || 'Falha na autenticação. Verifique as suas credenciais.';
      toast.error(message);
      throw error; // Lança o erro para o formulário saber que falhou
    }
  };

  const logout = () => {
    localStorage.removeItem('@SGMTA:token');
    setToken(null);
  };

  return (
    <AuthContext.Provider value={{ isAuthenticated: !!token, token, loading, login, logout }}>
      {children}
    </AuthContext.Provider>
  );
};