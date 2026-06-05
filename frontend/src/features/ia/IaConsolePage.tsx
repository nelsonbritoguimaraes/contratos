/**
 * IA Console — O "cérebro" do sistema (Fase 2)
 * Chat funcional com os 10 agentes via /api/ia/ask + seletor de agentes.
 * Onda 3 polish: integração real com location.state (CommandPalette + History replay),
 * uso completo do useIAsk (incluindo error/reset), estados aprimorados, prompts sugeridos conectados ao backend.
 */
import { useState, useEffect } from 'react'
import { Box, Typography, Paper, Alert, Stack, Chip, TextField, Button, CircularProgress, IconButton } from '@mui/material'
import { Send, Trash2 } from 'lucide-react'
import { useLocation } from 'react-router-dom'
import { useTenant } from '../../api/hooks/useTenant'
import { useIAsk } from '../../api/hooks/useIAsk'

const AGENTS = [
  'GlosaAgent', 'ContratoAgent', 'FolhaAgent', 'FiscalAgent',
  'ContabilAgent', 'ExecutivoAgent', 'LicitacoesAgent', 'PontoAgent',
  'DocumentAgent', 'EstoqueAgent'
]

interface Message {
  role: 'user' | 'assistant'
  content: string
  agents?: string[]
}

export default function IaConsolePage() {
  const { tenantName } = useTenant()
  const { mutateAsync: askIA, isPending, error: askError, reset: resetAsk } = useIAsk()
  const location = useLocation()

  const [messages, setMessages] = useState<Message[]>([])
  const [input, setInput] = useState('')
  const [selectedAgent, setSelectedAgent] = useState<string | null>(null)

  // Strengthen integration with CommandPalette and History replay (real router state)
  useEffect(() => {
    const state = location.state as { question?: string; iaResult?: any } | null
    if (state?.question) {
      setInput(state.question)
    }
    if (state?.iaResult) {
      const res = state.iaResult
      const assistantContent = Object.values(res.answers || {}).join('\n\n') || 'Resposta obtida via atalho de IA.'
      const initialMessages: Message[] = [
        { role: 'user', content: state.question || 'Pergunta via Command Palette' },
        {
          role: 'assistant',
          content: assistantContent,
          agents: res.routed_agents || []
        }
      ]
      setMessages(initialMessages)
    }
    // Clear navigation state after consuming (prevents re-seed on re-renders)
    if (state && (state.question || state.iaResult)) {
      window.history.replaceState({}, document.title)
    }
  }, []) // mount only — real backend results flow through here now

  const handleSend = async () => {
    if (!input.trim() || isPending) return

    const question = input.trim()
    const userMessage: Message = { role: 'user', content: question }
    setMessages(prev => [...prev, userMessage])
    setInput('')
    if (askError) resetAsk() // clear prior error state from real hook

    try {
      const response = await askIA({
        question,
        context: selectedAgent ? { preferredAgent: selectedAgent } : {}
      })

      const assistantMessage: Message = {
        role: 'assistant',
        content: Object.values(response.answers || {}).join('\n\n') || 'Não obtive resposta estruturada.',
        agents: response.routed_agents
      }
      setMessages(prev => [...prev, assistantMessage])
    } catch (error: any) {
      // Real error from backend AgentRouter / orchestrator is captured by mutation error
      // Fallback only if needed; primary path uses askError below for UI
      setMessages(prev => [...prev, {
        role: 'assistant',
        content: `Erro ao consultar a IA: ${error?.message || 'Tente novamente.'}`
      }])
    }
  }

  const handleClearChat = () => {
    setMessages([])
    if (askError) resetAsk()
    setInput('')
  }

  // Quick real prompts that trigger the actual backend agents/router (no demo data)
  const suggestedPrompts = [
    'Qual o risco de glosa no contrato atual?',
    'Resuma o impacto da última medição no fluxo de caixa',
    'Análise de cobertura de ponto vs escala contratual',
    'Sugestões de lançamentos contábeis para faturamento de serviços'
  ]

  const handleSuggested = (prompt: string) => {
    setInput(prompt)
    // Focus input naturally after selection
  }

  return (
    <Box>
      <Stack direction="row" justifyContent="space-between" alignItems="flex-start" mb={1}>
        <Box>
          <Typography variant="h4" fontWeight={600} gutterBottom>
            Console de Inteligência Artificial
          </Typography>
          <Typography color="text.secondary">
            10 Agentes especializados • AgentRouter inteligente • {tenantName}
          </Typography>
        </Box>
        {messages.length > 0 && (
          <IconButton onClick={handleClearChat} size="small" title="Limpar conversa" sx={{ mt: 0.5 }}>
            <Trash2 size={18} />
          </IconButton>
        )}
      </Stack>

      <Alert severity="info" sx={{ mb: 2 }}>
        Use <strong>Ctrl + K</strong> de qualquer tela para perguntas rápidas. O AgentRouter e os agentes reais (via /api/ia/ask) respondem aqui. Histórico persiste no backend.
      </Alert>

      {/* Real error state from useIAsk mutation — production feel, not injected in chat */}
      {askError && (
        <Alert
          severity="error"
          sx={{ mb: 2 }}
          onClose={() => resetAsk()}
        >
          Erro na consulta ao AgentRouter: {(askError as any)?.message || 'Falha na comunicação com o backend de IA. Verifique conexão e tente novamente.'}
        </Alert>
      )}

      {/* Seletor de Agentes — kept excellent as-is (Onda 3 polish preserves this) */}
      <Stack direction="row" spacing={1} flexWrap="wrap" mb={1.5}>
        <Chip
          label="Auto (Router)"
          color={!selectedAgent ? 'primary' : 'default'}
          onClick={() => setSelectedAgent(null)}
          clickable
        />
        {AGENTS.map(agent => (
          <Chip
            key={agent}
            label={agent}
            color={selectedAgent === agent ? 'primary' : 'default'}
            variant={selectedAgent === agent ? 'filled' : 'outlined'}
            onClick={() => setSelectedAgent(agent)}
            clickable
          />
        ))}
      </Stack>

      {/* Suggested prompts — real entry points to the 10-agent backend system (no demo responses) */}
      {messages.length === 0 && (
        <Stack direction="row" spacing={1} flexWrap="wrap" mb={2}>
          {suggestedPrompts.map((p, i) => (
            <Chip
              key={i}
              label={p.length > 55 ? p.slice(0, 52) + '...' : p}
              variant="outlined"
              size="small"
              onClick={() => handleSuggested(p)}
              clickable
              sx={{ maxWidth: 320 }}
            />
          ))}
        </Stack>
      )}

      {/* Chat Area */}
      <Paper sx={{ p: 2, borderRadius: 3, minHeight: 420, display: 'flex', flexDirection: 'column' }}>
        <Box sx={{ flex: 1, overflowY: 'auto', mb: 2, pr: 1 }}>
          {messages.length === 0 && (
            <Box sx={{ display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'center', height: '100%', color: 'text.secondary', fontSize: '0.9rem', textAlign: 'center', gap: 1 }}>
              <div>Conectado ao AgentRouter real + 10 agentes especializados.</div>
              <div>Use o seletor acima para forçar um agente ou deixe em Auto. Histórico gravado via /api/ia/calls.</div>
            </Box>
          )}
          {messages.map((msg, idx) => (
            <Box key={idx} sx={{ mb: 2.5 }}>
              <Typography variant="caption" color="text.secondary" fontWeight={600}>
                {msg.role === 'user' ? 'Você' : 'ContractOps AI'}
                {msg.agents && msg.agents.length > 0 && ` • ${msg.agents.join(', ')}`}
              </Typography>
              <Paper
                variant="outlined"
                sx={{
                  p: 1.5,
                  mt: 0.5,
                  bgcolor: msg.role === 'user' ? 'action.hover' : 'background.default',
                  whiteSpace: 'pre-wrap'
                }}
              >
                {msg.content}
              </Paper>
            </Box>
          ))}
          {isPending && (
            <Box sx={{ display: 'flex', alignItems: 'center', gap: 1, color: 'text.secondary' }}>
              <CircularProgress size={16} /> Pensando...
            </Box>
          )}
        </Box>

        {/* Input */}
        <Stack direction="row" spacing={1}>
          <TextField
            fullWidth
            placeholder="Pergunte algo aos agentes (glosas, folha, contratos, ponto, fiscal, etc)..."
            value={input}
            onChange={(e) => setInput(e.target.value)}
            onKeyDown={(e) => e.key === 'Enter' && !e.shiftKey && handleSend()}
            disabled={isPending}
            multiline
            maxRows={3}
          />
          <Button
            variant="contained"
            onClick={handleSend}
            disabled={!input.trim() || isPending}
            sx={{ minWidth: 56 }}
          >
            <Send size={18} />
          </Button>
        </Stack>
      </Paper>
    </Box>
  )
}
