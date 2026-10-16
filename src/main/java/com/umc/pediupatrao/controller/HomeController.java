package com.umc.pediupatrao.controller;

import com.umc.pediupatrao.dto.CriarPedidoRequest;
import com.umc.pediupatrao.dto.ItemPedidoRequest;
import com.umc.pediupatrao.entity.Cliente;
import com.umc.pediupatrao.entity.EventoAuditoria;
import com.umc.pediupatrao.entity.Pedido;
import com.umc.pediupatrao.entity.PerfilUsuario;
import com.umc.pediupatrao.entity.Produto;
import com.umc.pediupatrao.entity.StatusPedido;
import com.umc.pediupatrao.entity.Usuario;
import com.umc.pediupatrao.exception.RegraNegocioException;
import com.umc.pediupatrao.service.AuditoriaService;
import com.umc.pediupatrao.service.ClienteService;
import com.umc.pediupatrao.service.PedidoService;
import com.umc.pediupatrao.service.ProdutoService;
import com.umc.pediupatrao.service.UsuarioService;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class HomeController {
    private final PedidoService pedidoService;
    private final ClienteService clienteService;
    private final ProdutoService produtoService;
    private final UsuarioService usuarioService;
    private final AuditoriaService auditoriaService;

    public HomeController(PedidoService pedidoService, ClienteService clienteService,
            ProdutoService produtoService, UsuarioService usuarioService, AuditoriaService auditoriaService) {
        this.pedidoService = pedidoService;
        this.clienteService = clienteService;
        this.produtoService = produtoService;
        this.usuarioService = usuarioService;
        this.auditoriaService = auditoriaService;
    }

    @ModelAttribute("currentUser")
    public String currentUser(Authentication authentication) {
        return authentication == null ? "" : authentication.getName();
    }

    @ModelAttribute("isAdmin")
    public boolean isAdmin(Authentication authentication) { return possuiPerfil(authentication, "ADMIN"); }

    @ModelAttribute("isGerente")
    public boolean isGerente(Authentication authentication) { return possuiPerfil(authentication, "GERENTE"); }

    @ModelAttribute("isAtendente")
    public boolean isAtendente(Authentication authentication) { return possuiPerfil(authentication, "ATENDENTE"); }

    @GetMapping("/login")
    public String login() { return "login"; }

    @GetMapping("/acesso-negado")
    public String acessoNegado(Model model) {
        model.addAttribute("status", 403);
        model.addAttribute("message", "Seu perfil não tem permissão para esta operação.");
        return "error";
    }

    @GetMapping("/")
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE', 'ATENDENTE')")
    public String home(Model model) {
        model.addAttribute("totalPedidos", pedidoService.listarPedidos().size());
        model.addAttribute("content", "home :: content");
        return "layout";
    }

    @GetMapping("/usuarios")
    @PreAuthorize("hasRole('ADMIN')")
    public String listarUsuarios(Model model) {
        model.addAttribute("usuarios", usuarioService.listarTodos());
        model.addAttribute("content", "usuarios/lista :: content");
        return "layout";
    }

    @GetMapping("/usuarios/novo")
    @PreAuthorize("hasRole('ADMIN')")
    public String novoUsuarioForm(Model model) {
        model.addAttribute("usuario", new Usuario());
        model.addAttribute("perfis", PerfilUsuario.values());
        model.addAttribute("content", "usuarios/form :: content");
        return "layout";
    }

    @GetMapping("/usuarios/editar/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public String editarUsuarioForm(@PathVariable String id, Model model, RedirectAttributes redirectAttributes) {
        Usuario usuario = usuarioService.buscarPorId(id).orElse(null);
        if (usuario == null) {
            redirectAttributes.addFlashAttribute("erro", "Usuário não encontrado.");
            return "redirect:/usuarios";
        }
        try {
            usuario.setRole(PerfilUsuario.fromPersisted(usuario.getRole()).name());
        } catch (IllegalArgumentException exception) {
            usuario.setRole("");
        }
        model.addAttribute("usuario", usuario);
        model.addAttribute("perfis", PerfilUsuario.values());
        model.addAttribute("content", "usuarios/form :: content");
        return "layout";
    }

    @PostMapping("/usuarios/salvar")
    @PreAuthorize("hasRole('ADMIN')")
    public String salvarUsuario(@ModelAttribute Usuario usuario, RedirectAttributes redirectAttributes) {
        usuarioService.salvarUsuario(usuario);
        redirectAttributes.addFlashAttribute("sucesso", "Usuário criado com sucesso.");
        return "redirect:/usuarios";
    }

    @PostMapping("/usuarios/editar/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public String atualizarUsuario(@PathVariable String id, @ModelAttribute Usuario usuario,
            RedirectAttributes redirectAttributes) {
        usuario.setId(id);
        usuarioService.salvarUsuario(usuario);
        redirectAttributes.addFlashAttribute("sucesso", "Usuário atualizado com sucesso.");
        return "redirect:/usuarios";
    }

    @PostMapping("/usuarios/deletar/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public String deletarUsuario(@PathVariable String id, RedirectAttributes redirectAttributes) {
        usuarioService.deletarUsuario(id);
        redirectAttributes.addFlashAttribute("sucesso", "Usuário removido com sucesso.");
        return "redirect:/usuarios";
    }

    @GetMapping("/pedidos")
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE', 'ATENDENTE')")
    public String pedidos(Model model, Authentication authentication) {
        List<Pedido> pedidos = pedidoService.listarPedidos();
        Map<String, List<StatusPedido>> transicoes = new LinkedHashMap<>();
        for (Pedido pedido : pedidos) {
            try {
                transicoes.put(pedido.getId(), pedidoService.proximosStatus(pedido.getStatus()));
            } catch (RegraNegocioException exception) {
                transicoes.put(pedido.getId(), List.of());
            }
        }
        model.addAttribute("pedidos", pedidos);
        List<Cliente> clientes = List.of();
        if (possuiPerfil(authentication, "ADMIN")) {
            model.addAttribute("clientes", List.of());
            model.addAttribute("produtos", List.of());
        } else {
            clientes = clienteService.listarClientes();
            model.addAttribute("clientes", clientes);
            model.addAttribute("produtos", produtoService.listarProdutos().stream()
                    .filter(produto -> !Boolean.FALSE.equals(produto.getAtivo())).toList());
        }
        Map<String, String> nomesClientes = new LinkedHashMap<>();
        for (Cliente cliente : clientes) {
            nomesClientes.put(cliente.getId(), cliente.getNome());
        }
        model.addAttribute("nomesClientes", nomesClientes);
        model.addAttribute("transicoes", transicoes);
        model.addAttribute("content", "pedidos :: content");
        return "layout";
    }

    @PostMapping("/pedidos/novo")
    @PreAuthorize("hasAnyRole('GERENTE', 'ATENDENTE')")
    public String criarPedido(@RequestParam String clienteId,
            @RequestParam(required = false) List<String> produtoIds,
            @RequestParam(required = false) List<Integer> quantidades,
            RedirectAttributes redirectAttributes) {
        CriarPedidoRequest request = new CriarPedidoRequest();
        request.setClienteId(clienteId);
        List<ItemPedidoRequest> itens = new ArrayList<>();
        if (produtoIds != null) {
            for (int index = 0; index < produtoIds.size(); index++) {
                ItemPedidoRequest item = new ItemPedidoRequest();
                item.setProdutoId(produtoIds.get(index));
                item.setQuantidade(quantidades != null && index < quantidades.size() && quantidades.get(index) != null
                        ? quantidades.get(index) : 1);
                itens.add(item);
            }
        }
        request.setItens(itens);
        pedidoService.criarPedido(request);
        redirectAttributes.addFlashAttribute("sucesso", "Pedido registrado com sucesso.");
        return "redirect:/pedidos";
    }

    @PostMapping("/pedidos/{id}/status")
    @PreAuthorize("hasAnyRole('GERENTE', 'ATENDENTE')")
    public String atualizarStatus(@PathVariable String id, @RequestParam String status,
            RedirectAttributes redirectAttributes) {
        pedidoService.atualizarStatus(id, status);
        redirectAttributes.addFlashAttribute("sucesso", "Andamento do pedido atualizado.");
        return "redirect:/pedidos";
    }

    @PostMapping("/pedidos/{id}/desconto")
    @PreAuthorize("hasRole('GERENTE')")
    public String desconto(@PathVariable String id, @RequestParam BigDecimal percentual,
            @RequestParam(required = false) String justificativa, RedirectAttributes redirectAttributes) {
        pedidoService.aplicarDesconto(id, percentual, justificativa);
        redirectAttributes.addFlashAttribute("sucesso", "Desconto aplicado ao pedido.");
        return "redirect:/pedidos";
    }

    @PostMapping("/pedidos/{id}/cancelar")
    @PreAuthorize("hasRole('GERENTE')")
    public String cancelarPedido(@PathVariable String id, @RequestParam String justificativa,
            RedirectAttributes redirectAttributes) {
        pedidoService.cancelar(id, justificativa);
        redirectAttributes.addFlashAttribute("sucesso", "Pedido cancelado com justificativa registrada.");
        return "redirect:/pedidos";
    }

    @GetMapping("/produtos")
    @PreAuthorize("hasRole('GERENTE')")
    public String produtos(Model model) {
        model.addAttribute("produtos", produtoService.listarProdutos());
        model.addAttribute("content", "produtos/lista :: content");
        return "layout";
    }

    @PostMapping("/produtos/salvar")
    @PreAuthorize("hasRole('GERENTE')")
    public String salvarProduto(@ModelAttribute Produto produto, RedirectAttributes redirectAttributes) {
        produtoService.salvar(produto);
        redirectAttributes.addFlashAttribute("sucesso", "Produto salvo.");
        return "redirect:/produtos";
    }

    @PostMapping("/produtos/deletar/{id}")
    @PreAuthorize("hasRole('GERENTE')")
    public String excluirProduto(@PathVariable String id, RedirectAttributes redirectAttributes) {
        produtoService.excluir(id);
        redirectAttributes.addFlashAttribute("sucesso", "Produto removido.");
        return "redirect:/produtos";
    }

    @GetMapping("/clientes")
    @PreAuthorize("hasAnyRole('GERENTE', 'ATENDENTE')")
    public String clientes(Model model) {
        model.addAttribute("clientes", clienteService.listarClientes());
        model.addAttribute("content", "clientes/lista :: content");
        return "layout";
    }

    @GetMapping("/clientes/novo")
    @PreAuthorize("hasRole('ATENDENTE')")
    public String novoClienteForm(Model model) {
        model.addAttribute("cliente", new Cliente());
        model.addAttribute("content", "clientes/form :: content");
        return "layout";
    }

    @PostMapping("/clientes/salvar")
    @PreAuthorize("hasRole('ATENDENTE')")
    public String salvarCliente(@ModelAttribute Cliente cliente, RedirectAttributes redirectAttributes) {
        cliente.setId(null);
        clienteService.salvar(cliente);
        redirectAttributes.addFlashAttribute("sucesso", "Cliente cadastrado.");
        return "redirect:/clientes";
    }

    @GetMapping("/clientes/editar/{id}")
    @PreAuthorize("hasRole('ATENDENTE')")
    public String editarClienteForm(@PathVariable String id, Model model, RedirectAttributes redirectAttributes) {
        Cliente cliente = clienteService.buscarPorId(id).orElse(null);
        if (cliente == null) {
            redirectAttributes.addFlashAttribute("erro", "Cliente não encontrado.");
            return "redirect:/clientes";
        }
        model.addAttribute("cliente", cliente);
        model.addAttribute("content", "clientes/form :: content");
        return "layout";
    }

    @PostMapping("/clientes/editar/{id}")
    @PreAuthorize("hasRole('ATENDENTE')")
    public String atualizarCliente(@PathVariable String id, @ModelAttribute Cliente cliente,
            RedirectAttributes redirectAttributes) {
        cliente.setId(id);
        clienteService.salvar(cliente);
        redirectAttributes.addFlashAttribute("sucesso", "Dados do cliente atualizados.");
        return "redirect:/clientes";
    }

    @PostMapping("/clientes/deletar/{id}")
    @PreAuthorize("hasRole('GERENTE')")
    public String excluirCliente(@PathVariable String id, RedirectAttributes redirectAttributes) {
        clienteService.excluir(id);
        redirectAttributes.addFlashAttribute("sucesso", "Cliente removido.");
        return "redirect:/clientes";
    }

    @GetMapping("/auditoria")
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE')")
    public String auditoria(Model model) {
        List<EventoAuditoria> eventos = auditoriaService.listarRecentes();
        model.addAttribute("eventos", eventos);
        model.addAttribute("content", "auditoria :: content");
        return "layout";
    }

    @GetMapping("/configuracoes")
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE')")
    public String configuracoes(Model model) {
        model.addAttribute("content", "configuracoes :: content");
        return "layout";
    }

    private boolean possuiPerfil(Authentication authentication, String perfil) {
        return authentication != null && authentication.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_" + perfil));
    }
}



