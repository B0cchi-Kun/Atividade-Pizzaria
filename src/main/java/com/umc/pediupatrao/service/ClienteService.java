package com.umc.pediupatrao.service;

import com.umc.pediupatrao.entity.Cliente;
import com.umc.pediupatrao.exception.RegraNegocioException;
import com.umc.pediupatrao.repository.ClienteRepository;
import com.umc.pediupatrao.repository.PedidoRepository;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

@Service
public class ClienteService {
    private final ClienteRepository clienteRepository;
    private final AuditoriaService auditoriaService;
    private final PedidoRepository pedidoRepository;

    public ClienteService(ClienteRepository clienteRepository, AuditoriaService auditoriaService,
            PedidoRepository pedidoRepository) {
        this.clienteRepository = clienteRepository;
        this.auditoriaService = auditoriaService;
        this.pedidoRepository = pedidoRepository;
    }

    @PreAuthorize("hasAnyRole('GERENTE', 'ATENDENTE')")
    public List<Cliente> listarClientes() {
        return clienteRepository.findAll();
    }

    @PreAuthorize("hasAnyRole('GERENTE', 'ATENDENTE')")
    public Optional<Cliente> buscarPorId(String id) {
        return clienteRepository.findById(id);
    }

    @PreAuthorize("hasRole('ATENDENTE')")
    public Cliente salvar(Cliente recebido) {
        validarCliente(recebido);
        if (recebido.getId() == null || recebido.getId().isBlank()) {
            recebido.setId(null);
            Cliente salvo = clienteRepository.save(recebido);
            auditoriaService.registrar("CRIACAO_CLIENTE", "Cliente", salvo.getId(),
                    Map.of(), snapshot(salvo), null);
            return salvo;
        }

        Cliente anterior = clienteRepository.findById(recebido.getId())
                .orElseThrow(() -> new RegraNegocioException("Cliente não encontrado para atualização."));

        Map<String, Object> dadosAnteriores = snapshot(anterior);
        Map<String, Object> dadosNovos = snapshot(recebido);
        Map<String, Object> valoresAnteriores = new LinkedHashMap<>();
        Map<String, Object> valoresNovos = new LinkedHashMap<>();
        for (String campo : dadosAnteriores.keySet()) {
            Object valorAnterior = dadosAnteriores.get(campo);
            Object valorNovo = dadosNovos.get(campo);
            if (!Objects.equals(valorAnterior, valorNovo)) {
                valoresAnteriores.put(campo, valorAnterior);
                valoresNovos.put(campo, valorNovo);
            }
        }

        recebido.setId(anterior.getId());
        Cliente salvo = clienteRepository.save(recebido);
        if (!valoresAnteriores.isEmpty()) {
            auditoriaService.registrar("ALTERACAO_CLIENTE", "Cliente", salvo.getId(),
                    valoresAnteriores, valoresNovos, null);
        }
        return salvo;
    }

    @PreAuthorize("hasRole('GERENTE')")
    public void excluir(String id) {
        Cliente cliente = clienteRepository.findById(id)
                .orElseThrow(() -> new RegraNegocioException("Cliente não encontrado."));
        if (!pedidoRepository.findByClienteId(id).isEmpty()) {
            throw new RegraNegocioException("O cliente possui pedidos associados e não pode ser removido.");
        }
        auditoriaService.registrar("EXCLUSAO_CLIENTE", "Cliente", id,
                snapshot(cliente), Map.of(), null);
        clienteRepository.delete(cliente);
    }

    private void validarCliente(Cliente cliente) {
        if (cliente == null || cliente.getNome() == null || cliente.getNome().isBlank()) {
            throw new RegraNegocioException("Informe o nome do cliente.");
        }
        if (cliente.getTelefone() == null || cliente.getTelefone().isBlank()) {
            throw new RegraNegocioException("Informe o telefone do cliente.");
        }

        cliente.setNome(cliente.getNome().trim());
        cliente.setTelefone(cliente.getTelefone().trim());
        if (cliente.getCidade() != null) {
            cliente.setCidade(cliente.getCidade().trim());
        }
    }

    private Map<String, Object> snapshot(Cliente cliente) {
        Map<String, Object> valores = new LinkedHashMap<>();
        valores.put("nome", cliente.getNome());
        valores.put("telefone", cliente.getTelefone());
        valores.put("cep", cliente.getCep());
        valores.put("logradouro", cliente.getLogradouro());
        valores.put("numero", cliente.getNumero());
        valores.put("complemento", cliente.getComplemento());
        valores.put("bairro", cliente.getBairro());
        valores.put("cidade", cliente.getCidade());
        valores.put("estado", cliente.getEstado());
        valores.put("endereco", cliente.getEndereco());
        return valores;
    }
}
