package com.umc.pediupatrao.service;

import com.umc.pediupatrao.entity.Produto;
import com.umc.pediupatrao.exception.RegraNegocioException;
import com.umc.pediupatrao.repository.ProdutoRepository;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

@Service
public class ProdutoService {
    private final ProdutoRepository produtoRepository;

    public ProdutoService(ProdutoRepository produtoRepository) {
        this.produtoRepository = produtoRepository;
    }

    @PreAuthorize("hasAnyRole('GERENTE', 'ATENDENTE')")
    public List<Produto> listarProdutos() {
        return produtoRepository.findAll();
    }

    @PreAuthorize("hasRole('GERENTE')")
    public void excluir(String id) {
        if (!produtoRepository.existsById(id)) {
            throw new RegraNegocioException("Produto não encontrado.");
        }
        produtoRepository.deleteById(id);
    }

    @PreAuthorize("hasRole('GERENTE')")
    public Produto salvar(Produto produto) {
        if (produto.getNome() == null || produto.getNome().isBlank()) {
            throw new RegraNegocioException("Informe o nome do produto.");
        }
        if (produto.getPreco() == null || !Double.isFinite(produto.getPreco()) || produto.getPreco() <= 0) {
            throw new RegraNegocioException("Informe um preço maior que zero.");
        }
        if (produto.getId() == null || produto.getId().isBlank()) {
            produto.setId(null);
            if (produto.getAtivo() == null) {
                produto.setAtivo(true);
            }
            return produtoRepository.save(produto);
        }
        if (!produtoRepository.existsById(produto.getId())) {
            throw new RegraNegocioException("Produto não encontrado para atualização.");
        }
        return produtoRepository.save(produto);
    }
}

